package com.company.platform.user;

import com.company.platform.commons.dto.UserDto;
import com.company.platform.commons.enums.RoleType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
class AuthUserAdminService {
    private final String url;
    private final String username;
    private final String password;

    AuthUserAdminService(@Value("${auth.datasource.url:${AUTH_DATABASE_URL:}}") String url,
                         @Value("${auth.datasource.username:${AUTH_DATABASE_USERNAME:}}") String username,
                         @Value("${auth.datasource.password:${AUTH_DATABASE_PASSWORD:}}") String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    List<UserDto> search(String query, String role) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        String sql = """
                SELECT u.id, u.full_name, u.email, u.avatar_url, COALESCE(string_agg(r.name, ',' ORDER BY r.name), '') AS roles
                FROM users u
                LEFT JOIN user_roles ur ON ur.user_id = u.id
                LEFT JOIN roles r ON r.id = ur.role_id
                WHERE (? = '' OR lower(coalesce(u.full_name, '')) LIKE ? OR lower(u.email) LIKE ?)
                GROUP BY u.id, u.full_name, u.email, u.avatar_url
                HAVING (? = '' OR bool_or(r.name = ?))
                ORDER BY lower(u.email)
                LIMIT 250
                """;
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String like = "%" + normalizedQuery + "%";
            statement.setString(1, normalizedQuery);
            statement.setString(2, like);
            statement.setString(3, like);
            statement.setString(4, normalizedRole);
            statement.setString(5, normalizedRole);
            try (ResultSet rs = statement.executeQuery()) {
                List<UserDto> users = new ArrayList<>();
                while (rs.next()) {
                    users.add(toUser(rs));
                }
                return users;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to search auth users", ex);
        }
    }

    UserDto get(UUID userId) {
        return search("", "").stream()
                .filter(user -> userId.equals(user.getUserId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    UserDto updateRoles(UUID userId, Set<RoleType> requestedRoles) {
        Set<RoleType> roles = requestedRoles == null || requestedRoles.isEmpty()
                ? Set.of(RoleType.USER)
                : requestedRoles;
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            ensureRoles(connection, roles);
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM user_roles WHERE user_id = ?")) {
                delete.setObject(1, userId);
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO user_roles(user_id, role_id)
                    SELECT ?, id FROM roles WHERE name = ?
                    ON CONFLICT DO NOTHING
                    """)) {
                for (RoleType role : roles) {
                    insert.setObject(1, userId);
                    insert.setString(2, role.name());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            connection.commit();
            return findById(connection, userId);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to update user roles", ex);
        }
    }

    private void ensureRoles(Connection connection, Set<RoleType> roles) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO roles(name) VALUES (?) ON CONFLICT DO NOTHING")) {
            for (RoleType role : roles) {
                statement.setString(1, role.name());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private UserDto findById(Connection connection, UUID userId) throws SQLException {
        String sql = """
                SELECT u.id, u.full_name, u.email, u.avatar_url, COALESCE(string_agg(r.name, ',' ORDER BY r.name), '') AS roles
                FROM users u
                LEFT JOIN user_roles ur ON ur.user_id = u.id
                LEFT JOIN roles r ON r.id = ur.role_id
                WHERE u.id = ?
                GROUP BY u.id, u.full_name, u.email, u.avatar_url
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return toUser(rs);
                }
            }
        }
        throw new NoSuchElementException("User not found");
    }

    private UserDto toUser(ResultSet rs) throws SQLException {
        UUID userId = (UUID) rs.getObject("id");
        String email = rs.getString("email");
        String name = rs.getString("full_name");
        UserDto user = new UserDto();
        user.setUserId(userId);
        user.setEmail(email);
        user.setName(name == null || name.isBlank() ? displayName(email) : name);
        user.setAvatarUrl(null);
        Set<RoleType> roles = Arrays.stream(rs.getString("roles").split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(RoleType::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(RoleType.class)));
        user.setRoles(roles.isEmpty() ? Set.of(RoleType.USER) : roles);
        return user;
    }

    private String displayName(String email) {
        int atIndex = email == null ? -1 : email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : "User";
    }

    private Connection connection() throws SQLException {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("AUTH_DATABASE_URL is not configured for user-service admin operations");
        }
        return DriverManager.getConnection(url, username, password);
    }
}
