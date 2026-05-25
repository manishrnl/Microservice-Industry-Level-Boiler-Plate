package com.company.platform.user.dto;

import com.company.platform.commons.enums.RoleType;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

class UserDtoNormalizationTest {

    @Test
    void roleUpdateRequestCombinesRoleAndRolesCollection() {
        UserRoleUpdateRequestDto request = new UserRoleUpdateRequestDto();
        request.setRoles(List.of("admin", " user "));
        request.setRole("super_admin");

        assertEquals(request.toRoleTypes(), Set.of(RoleType.ADMIN, RoleType.USER, RoleType.SUPER_ADMIN));
    }

    @Test
    void roleUpdateRequestDefaultsToUserWhenEmpty() {
        assertEquals(new UserRoleUpdateRequestDto(Set.of(), null).toRoleTypes(), Set.of(RoleType.USER));
    }

    @Test
    void accountSettingsUpdateRequestNormalizesSensitiveInputs() {
        UserAccountSettingsUpdateRequestDto request = new UserAccountSettingsUpdateRequestDto();
        request.setUsername(" MANISH ");
        request.setAadhaarNumber("1234 5678 9012");
        request.setPanNumber(" abcde1234f ");
        request.setPhoneNumber("  +91 99999 99999 ");
        request.setPostalCode(" 110001 ");

        assertEquals(request.getUsername(), "manish");
        assertEquals(request.getAadhaarNumber(), "123456789012");
        assertEquals(request.getPanNumber(), "ABCDE1234F");
        assertEquals(request.getPhoneNumber(), "+91 99999 99999");
        assertEquals(request.getPostalCode(), "110001");
    }
}
