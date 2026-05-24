package com.company.platform.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.company.platform.user.security.EncryptedStringConverter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_identity_documents")
public class UserIdentityDocument {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "aadhaar_number")
    @Convert(converter = EncryptedStringConverter.class)
    private String aadhaarNumber;

    @Column(name = "pan_number")
    @Convert(converter = EncryptedStringConverter.class)
    private String panNumber;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
