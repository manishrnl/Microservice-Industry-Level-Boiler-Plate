package com.company.platform.user.auth.repository;

import com.company.platform.user.entity.AuthRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthRoleRepository extends JpaRepository<AuthRole, UUID> {
    Optional<AuthRole> findByName(String name);

    List<AuthRole> findByNameIn(Collection<String> names);
}
