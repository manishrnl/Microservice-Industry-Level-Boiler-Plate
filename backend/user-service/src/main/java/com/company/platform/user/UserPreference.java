package com.company.platform.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "user_preferences")
class UserPreference {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String timezone;
}
