package com.company.platform.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import com.company.platform.user.security.EncryptedStringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Convert(converter = EncryptedStringConverter.class)
    private String name;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String avatarUrl;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "aadhaar_number")
    @Convert(converter = EncryptedStringConverter.class)
    private String aadhaarNumber;

    @Column(name = "pan_number")
    @Convert(converter = EncryptedStringConverter.class)
    private String panNumber;

    @Column(name = "phone_number")
    @Convert(converter = EncryptedStringConverter.class)
    private String phoneNumber;

    @Column(name = "address_line", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String addressLine;

    @Convert(converter = EncryptedStringConverter.class)
    private String city;

    @Convert(converter = EncryptedStringConverter.class)
    private String state;

    @Convert(converter = EncryptedStringConverter.class)
    private String country;

    @Column(name = "postal_code")
    @Convert(converter = EncryptedStringConverter.class)
    private String postalCode;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
