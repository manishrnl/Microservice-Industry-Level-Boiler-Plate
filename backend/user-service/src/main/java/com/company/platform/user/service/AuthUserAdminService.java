package com.company.platform.user.service;

import com.company.platform.user.model.UserProfile;
import com.company.platform.user.repository.UserProfileRepository;
import com.company.platform.commons.dto.UserDto;
import com.company.platform.commons.enums.RoleType;
import com.company.platform.commons.exception.ApiExceptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthUserAdminService {
    private final String url;
    private final String username;
    private final String password;
    private final UserProfileRepository profiles;

    public AuthUserAdminService(@Value("${auth.datasource.url:${AUTH_DATABASE_URL:}}") String url,
                         @Value("${auth.datasource.username:${AUTH_DATABASE_USERNAME:}}") String username,
                         @Value("${auth.datasource.password:${AUTH_DATABASE_PASSWORD:}}") String password,
                         UserProfileRepository profiles) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.profiles = profiles;
    }

    public List<UserDto> search(String query, String role) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        List<AuthAccount> accounts = searchAuthAccounts(role);
        Map<UUID, UserProfile> profileById = profiles.findAllById(accounts.stream().map(AuthAccount::userId).toList())
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, profile -> profile));
        return accounts.stream()
                .map(account -> toUser(account, profileById.get(account.userId())))
                .filter(user -> matches(user, normalizedQuery))
                .limit(250)
                .toList();
    }

    public UserDto get(UUID userId) {
        AuthAccount account = account(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
        UserProfile profile = profiles.findById(userId).orElse(null);
        return toUser(account, profile);
    }

    public Optional<AuthAccount> account(UUID userId) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT u.id, u.email, u.username, u.provider, u.email_verified, u.account_locked,
                            u.account_status, u.locked_until, u.deleted_at,
                            COALESCE(string_agg(r.name, ',' ORDER BY r.name), '') AS roles
                     FROM users u
                     LEFT JOIN user_roles ur ON ur.user_id = u.id
                     LEFT JOIN roles r ON r.id = ur.role_id
                     WHERE u.id = ?
                     GROUP BY u.id, u.email, u.username, u.provider, u.email_verified, u.account_locked,
                              u.account_status, u.locked_until, u.deleted_at
                     """)) {
            statement.setObject(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(toAccount(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read auth user", ex);
        }
        return Optional.empty();
    }

    public UserDto updateRoles(UUID userId, Set<RoleType> requestedRoles) {
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
            return get(userId);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to update user roles", ex);
        }
    }

    public void updateUsername(UUID userId, String username) {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase();
        if (normalizedUsername.isBlank()) {
            return;
        }
        try (Connection connection = connection()) {
            try (PreparedStatement duplicate = connection.prepareStatement("SELECT 1 FROM users WHERE lower(username) = ? AND id <> ? LIMIT 1")) {
                duplicate.setString(1, normalizedUsername);
                duplicate.setObject(2, userId);
                try (ResultSet rs = duplicate.executeQuery()) {
                    if (rs.next()) {
                        throw new ApiExceptions.ConflictException("Username already registered");
                    }
                }
            }
            try (PreparedStatement update = connection.prepareStatement("UPDATE users SET username = ? WHERE id = ?")) {
                update.setString(1, normalizedUsername);
                update.setObject(2, userId);
                update.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to update username", ex);
        }
    }

    private List<AuthAccount> searchAuthAccounts(String role) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        String sql = """
                SELECT u.id, u.email, u.username, u.provider, u.email_verified, u.account_locked,
                       u.account_status, u.locked_until, u.deleted_at,
                       COALESCE(string_agg(r.name, ',' ORDER BY r.name), '') AS roles
                FROM users u
                LEFT JOIN user_roles ur ON ur.user_id = u.id
                LEFT JOIN roles r ON r.id = ur.role_id
                GROUP BY u.id, u.email, u.username, u.provider, u.email_verified, u.account_locked,
                         u.account_status, u.locked_until, u.deleted_at
                HAVING (? = '' OR bool_or(r.name = ?))
                ORDER BY lower(u.email)
                LIMIT 1000
                """;
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedRole);
            statement.setString(2, normalizedRole);
            try (ResultSet rs = statement.executeQuery()) {
                List<AuthAccount> users = new ArrayList<>();
                while (rs.next()) {
                    users.add(toAccount(rs));
                }
                return users;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to search auth users", ex);
        }
    }

    private void ensureRoles(Connection connection, Set<RoleType> roles) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO roles(id, name) VALUES (?, ?) ON CONFLICT (name) DO NOTHING")) {
            for (RoleType role : roles) {
                statement.setObject(1, UUID.randomUUID());
                statement.setString(2, role.name());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private AuthAccount toAccount(ResultSet rs) throws SQLException {
        return new AuthAccount(
                (UUID) rs.getObject("id"),
                rs.getString("email"),
                rs.getString("username"),
                rs.getString("provider"),
                rs.getBoolean("email_verified"),
                rs.getBoolean("account_locked"),
                rs.getString("account_status"),
                timestamp(rs, "locked_until"),
                timestamp(rs, "deleted_at"),
                roles(rs.getString("roles"))
        );
    }

    private UserDto toUser(AuthAccount account, UserProfile profile) {
        UserDto user = new UserDto();
        user.setUserId(account.userId());
        user.setEmail(account.email());
        user.setUsername(account.username());
        user.setName(displayName(profile == null ? null : profile.getName(), account.username(), account.email()));
        user.setAvatarUrl(profile == null ? null : profile.getAvatarUrl());
        user.setRoles(account.roles().isEmpty() ? Set.of(RoleType.USER) : account.roles());
        return user;
    }

    private boolean matches(UserDto user, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return contains(user.getName(), query)
                || contains(user.getEmail(), query)
                || contains(user.getUserId().toString(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private String displayName(String profileName, String username, String email) {
        if (profileName != null && !profileName.isBlank()) {
            return profileName;
        }
        if (username != null && !username.isBlank() && !username.equalsIgnoreCase(email)) {
            return username;
        }
        int atIndex = email == null ? -1 : email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : "User";
    }

    private Set<RoleType> roles(String roles) {
        Set<RoleType> parsed = Arrays.stream((roles == null ? "" : roles).split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(RoleType::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(RoleType.class)));
        return parsed.isEmpty() ? Set.of(RoleType.USER) : parsed;
    }

    private LocalDateTime timestamp(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Connection connection() throws SQLException {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("AUTH_DATABASE_URL is not configured for user-service admin operations");
        }
        return DriverManager.getConnection(url, username, password);
    }

    public record AuthAccount(UUID userId,
                       String email,
                       String username,
                       String provider,
                       boolean emailVerified,
                       boolean accountLocked,
                       String accountStatus,
                       LocalDateTime lockedUntil,
                       LocalDateTime deletedAt,
                       Set<RoleType> roles) {
    }
}
