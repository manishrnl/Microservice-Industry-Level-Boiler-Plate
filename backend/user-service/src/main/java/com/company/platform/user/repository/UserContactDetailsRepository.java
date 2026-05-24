package com.company.platform.user.repository;

import com.company.platform.user.model.UserContactDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserContactDetailsRepository extends JpaRepository<UserContactDetails, UUID> {
}
