package com.company.platform.user.auth.repository;

import com.company.platform.user.entity.AuthUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {
    @EntityGraph(attributePaths = "roles")
    List<AuthUser> findAllByOrderByEmailAsc(Pageable pageable);

    @EntityGraph(attributePaths = "roles")
    List<AuthUser> findByRolesNameOrderByEmailAsc(String role, Pageable pageable);

    @EntityGraph(attributePaths = "roles")
    Optional<AuthUser> findOneById(UUID id);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID id);
}
