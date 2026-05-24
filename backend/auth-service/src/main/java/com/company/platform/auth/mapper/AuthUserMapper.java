package com.company.platform.auth.mapper;

import com.company.platform.auth.entity.User;
import com.company.platform.commons.dto.UserDto;
import com.company.platform.commons.enums.RoleType;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class AuthUserMapper {
    private final ModelMapper modelMapper;

    public AuthUserMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public UserDto toDto(User user, Set<RoleType> roles) {
        String name = user.getUsername() == null || user.getUsername().isBlank()
                ? fallbackName(user.getEmail())
                : user.getUsername();
        UserDto dto = toDto(user.getId(), name, user.getEmail(), roles, null);
        dto.setUsername(user.getUsername());
        return dto;
    }

    public UserDto toDto(UUID userId, String name, String email, Set<RoleType> roles, String avatarUrl) {
        UserDto dto = modelMapper.map(new UserView(name, email, avatarUrl), UserDto.class);
        dto.setUserId(userId);
        dto.setRoles(roles);
        return dto;
    }

    private static final class UserView {
        private final String name;
        private final String email;
        private final String avatarUrl;

        private UserView(String name, String email, String avatarUrl) {
            this.name = name;
            this.email = email;
            this.avatarUrl = avatarUrl;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }
    }

    private String fallbackName(String email) {
        int atIndex = email == null ? -1 : email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : "User";
    }
}
