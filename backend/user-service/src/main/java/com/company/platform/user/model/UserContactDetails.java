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
@Table(name = "user_contact_details")
public class UserContactDetails {
    @Id
    @Column(name = "user_id")
    private UUID userId;

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

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
