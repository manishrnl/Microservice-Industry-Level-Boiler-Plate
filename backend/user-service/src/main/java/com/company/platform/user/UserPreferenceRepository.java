package com.company.platform.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {
}
