package com.company.platform.user.mapper;

import com.company.platform.commons.config.ModelMapperConfig;
import com.company.platform.commons.dto.UserDto;
import com.company.platform.commons.enums.RoleType;
import com.company.platform.user.dto.AuthAccountDto;
import com.company.platform.user.dto.UserAccountSettingsDto;
import com.company.platform.user.dto.UserHeaderContextDto;
import com.company.platform.user.entity.AuthRole;
import com.company.platform.user.entity.AuthUser;
import com.company.platform.user.model.UserContactDetails;
import com.company.platform.user.model.UserIdentityDocument;
import com.company.platform.user.model.UserProfile;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.*;

class UserAccountMapperTest {
    private UserAccountMapper mapper;

    @BeforeMethod
    void setUp() {
        mapper = new UserAccountMapper(new ModelMapperConfig().modelMapper());
        mapper.configureMappings();
    }

    @Test
    void authUserMapsToAuthAccountDtoWithRoleTypes() {
        UUID userId = UUID.randomUUID();
        AuthUser user = AuthUser.builder()
                .email("manish@example.com")
                .username("manish")
                .provider("LOCAL")
                .emailVerified(true)
                .roles(Set.of(AuthRole.builder().id(UUID.randomUUID()).name("ADMIN").build()))
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        AuthAccountDto dto = mapper.toAuthAccountDto(user);

        assertEquals(dto.getUserId(), userId);
        assertEquals(dto.getEmail(), "manish@example.com");
        assertEquals(dto.getRoles(), Set.of(RoleType.ADMIN));
    }

    @Test
    void userDtoPrefersProfileNameThenUsernameThenEmailLocalPart() {
        UUID userId = UUID.randomUUID();
        AuthAccountDto account = AuthAccountDto.builder()
                .userId(userId)
                .email("manish@example.com")
                .username("admin")
                .roles(Set.of(RoleType.ADMIN))
                .build();
        UserProfile profile = UserProfile.builder().userId(userId).name("MANISH").avatarUrl("/avatar.png").build();

        UserDto withProfile = mapper.toUserDto(account, profile);
        UserDto withUsername = mapper.toUserDto(account, null);
        account.setUsername("manish@example.com");
        UserDto withEmail = mapper.toUserDto(account, null);

        assertEquals(withProfile.getName(), "MANISH");
        assertEquals(withProfile.getAvatarUrl(), "/avatar.png");
        assertEquals(withUsername.getName(), "admin");
        assertEquals(withEmail.getName(), "manish");
    }

    @Test
    void headerContextMapsToUserDtoAndParsesRoles() {
        UUID userId = UUID.randomUUID();
        UserHeaderContextDto context = UserHeaderContextDto.builder()
                .userId(userId)
                .email("user@example.com")
                .name("Header Name")
                .roles("ADMIN,SUPER_ADMIN")
                .build();

        UserDto dto = mapper.toUserDto(null, context, "username");

        assertEquals(dto.getUserId(), userId);
        assertEquals(dto.getName(), "Header Name");
        assertEquals(dto.getUsername(), "username");
        assertTrue(String.valueOf(dto.getRoles()).contains(String.valueOf(RoleType.ADMIN)));
        assertTrue(String.valueOf(dto.getRoles()).contains(String.valueOf(RoleType.SUPER_ADMIN)));
    }

    @Test
    void accountSettingsMergesProfileIdentityContactAndAuthAccount() {
        UUID userId = UUID.randomUUID();
        UserHeaderContextDto context = UserHeaderContextDto.builder()
                .userId(userId)
                .email("header@example.com")
                .name("Header")
                .roles("USER")
                .build();
        AuthAccountDto account = AuthAccountDto.builder()
                .userId(userId)
                .email("auth@example.com")
                .username("authuser")
                .provider("LOCAL")
                .emailVerified(true)
                .accountStatus("")
                .roles(Set.of(RoleType.ADMIN))
                .build();
        UserProfile profile = UserProfile.builder().userId(userId).name("Profile").avatarUrl("/a.png")
                .dateOfBirth(LocalDate.parse("1990-01-01")).build();
        UserIdentityDocument identity = UserIdentityDocument.builder().userId(userId)
                .aadhaarNumber("123456789012").panNumber("ABCDE1234F").build();
        UserContactDetails contact = UserContactDetails.builder().userId(userId)
                .phoneNumber("+919999999999").country("").city("Delhi").build();

        UserAccountSettingsDto dto = mapper.toAccountSettingsDto(context, account, profile, identity, contact);

        assertEquals(dto.getName(), "Profile");
        assertEquals(dto.getEmail(), "auth@example.com");
        assertEquals(dto.getUsername(), "authuser");
        assertEquals(dto.getAccountStatus(), "ACTIVE");
        assertEquals(dto.getRoles(), Set.of(RoleType.ADMIN));
        assertEquals(dto.getAadhaarNumber(), "123456789012");
        assertEquals(dto.getCountry(), "India");
    }

    @Test
    void parsesDefaultRolesAndDemoUserDto() {
        UserProfile profile = UserProfile.builder().userId(UUID.randomUUID()).name("Demo").avatarUrl("/demo.png").build();

        assertEquals(mapper.parseRoles(null), Set.of(RoleType.USER));
        assertEquals(mapper.defaultRoles(Set.of()), Set.of(RoleType.USER));
        assertEquals(mapper.toDemoUserDto(profile, "demo@example.com", "demo").getRoles(), Set.of(RoleType.USER));
    }
}
