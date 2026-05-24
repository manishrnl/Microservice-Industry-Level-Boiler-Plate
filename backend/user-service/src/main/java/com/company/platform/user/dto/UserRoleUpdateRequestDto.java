package com.company.platform.user.dto;

import com.company.platform.commons.enums.RoleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleUpdateRequestDto {
    private Set<String> roles;
    private String role;

    public Set<RoleType> toRoleTypes() {
        Stream<String> roleStream = roles == null ? Stream.empty() : roles.stream();
        Set<RoleType> requestedRoles = Stream.concat(roleStream, Stream.of(role))
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase())
                .map(RoleType::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return requestedRoles.isEmpty() ? Set.of(RoleType.USER) : requestedRoles;
    }

    public void setRoles(Collection<String> roles) {
        this.roles = roles == null ? Set.of() : new LinkedHashSet<>(roles);
    }
}
