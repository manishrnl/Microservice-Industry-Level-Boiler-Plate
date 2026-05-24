package com.company.platform.user.repository;

import com.company.platform.user.model.UserIdentityDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserIdentityDocumentRepository extends JpaRepository<UserIdentityDocument, UUID> {
}
