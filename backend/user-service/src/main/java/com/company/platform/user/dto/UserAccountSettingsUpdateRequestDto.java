package com.company.platform.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserAccountSettingsUpdateRequestDto {
    @Size(max = 255)
    private String name;

    @Size(max = 120)
    @Pattern(regexp = "^[A-Za-z0-9._@-]*$", message = "Username can use letters, numbers, dot, underscore, at sign and hyphen")
    private String username;

    @Pattern(regexp = "^$|^[0-9]{12}$", message = "Aadhaar must be 12 digits")
    private String aadhaarNumber;

    @Pattern(regexp = "^$|^[A-Z]{5}[0-9]{4}[A-Z]$", message = "PAN must look like ABCDE1234F")
    private String panNumber;

    @Pattern(regexp = "^$|^[+]?[0-9 ()-]{7,20}$", message = "Phone number format is invalid")
    private String phoneNumber;

    private LocalDate dateOfBirth;

    @Size(max = 500)
    private String addressLine;

    @Size(max = 120)
    private String city;

    @Size(max = 120)
    private String state;

    @Size(max = 120)
    private String country;

    @Size(max = 20)
    private String postalCode;

    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = digitsOnly(aadhaarNumber);
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = uppercaseTrimmed(panNumber);
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = trimmed(phoneNumber);
    }

    public void setUsername(String username) {
        String normalized = trimmed(username);
        this.username = normalized == null ? null : normalized.toLowerCase();
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = trimmed(postalCode);
    }

    private String digitsOnly(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    private String uppercaseTrimmed(String value) {
        String normalized = trimmed(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String trimmed(String value) {
        return value == null ? null : value.trim();
    }
}
