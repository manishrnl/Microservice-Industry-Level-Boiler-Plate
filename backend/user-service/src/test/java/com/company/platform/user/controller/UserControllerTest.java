package com.company.platform.user.controller;

import com.company.platform.commons.config.ModelMapperConfig;
import com.company.platform.commons.dto.DemoUserRequestDto;
import com.company.platform.commons.dto.UserDto;
import com.company.platform.commons.enums.RoleType;
import com.company.platform.user.dto.AuthAccountDto;
import com.company.platform.user.dto.AvatarUpdateRequestDto;
import com.company.platform.user.dto.UserAccountSettingsUpdateRequestDto;
import com.company.platform.user.dto.UserPreferenceUpdateRequestDto;
import com.company.platform.user.dto.UserProfileUpdateRequestDto;
import com.company.platform.user.dto.UserRoleUpdateRequestDto;
import com.company.platform.user.mapper.UserAccountMapper;
import com.company.platform.user.model.UserContactDetails;
import com.company.platform.user.model.UserIdentityDocument;
import com.company.platform.user.model.UserPreference;
import com.company.platform.user.model.UserProfile;
import com.company.platform.user.repository.UserContactDetailsRepository;
import com.company.platform.user.repository.UserIdentityDocumentRepository;
import com.company.platform.user.repository.UserPreferenceRepository;
import com.company.platform.user.repository.UserProfileRepository;
import com.company.platform.user.service.AuthUserAdminService;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

class UserControllerTest {
    @Mock
    private UserPreferenceRepository preferences;
    @Mock
    private AuthUserAdminService authUsers;
    @Mock
    private UserProfileRepository profiles;
    @Mock
    private UserIdentityDocumentRepository identityDocuments;
    @Mock
    private UserContactDetailsRepository contactDetails;

    private UserController controller;

    @BeforeMethod
    void setUp() {
        MockitoAnnotations.openMocks(this);
        UserAccountMapper mapper = new UserAccountMapper(new ModelMapperConfig().modelMapper());
        ReflectionTestUtils.invokeMethod(mapper, "configureMappings");
        controller = new UserController(preferences, authUsers, profiles, identityDocuments, contactDetails, mapper);
    }

    @Test
    void meUsesProfileNameAndAuthUsernameForHeaderContext() {
        UUID userId = UUID.randomUUID();
        given(profiles.findById(userId)).willReturn(Optional.of(UserProfile.builder().userId(userId).name("MANISH").avatarUrl("/a.png").build()));
        given(authUsers.account(userId)).willReturn(Optional.of(AuthAccountDto.builder()
                .userId(userId)
                .email("manish@example.com")
                .username("manish")
                .roles(Set.of(RoleType.ADMIN))
                .build()));

        UserDto dto = controller.me(userId, "manish@example.com", "Header", "ADMIN");

        assertEquals(dto.getName(), "MANISH");
        assertEquals(dto.getUsername(), "manish");
        assertEquals(dto.getAvatarUrl(), "/a.png");
    }

    @Test
    void updateMePersistsDisplayNameAndReturnsMappedUser() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.builder().userId(userId).build();
        given(profiles.findById(userId)).willReturn(Optional.of(profile));
        given(authUsers.account(userId)).willReturn(Optional.empty());

        UserDto dto = controller.updateMe(userId, "manish@example.com", null, "USER",
                UserProfileUpdateRequestDto.builder().name("MANISH").build());

        assertEquals(profile.getName(), "MANISH");
        assertEquals(dto.getName(), "MANISH");
        verify(profiles).save(profile);
    }

    @Test
    void accountSettingsCombinesAuthProfileIdentityAndContactData() {
        UUID userId = UUID.randomUUID();
        given(authUsers.account(userId)).willReturn(Optional.of(AuthAccountDto.builder()
                .userId(userId)
                .email("auth@example.com")
                .username("authuser")
                .provider("LOCAL")
                .emailVerified(true)
                .roles(Set.of(RoleType.ADMIN))
                .build()));
        given(profiles.findById(userId)).willReturn(Optional.of(UserProfile.builder().userId(userId).name("MANISH").build()));
        given(identityDocuments.findById(userId)).willReturn(Optional.of(UserIdentityDocument.builder().userId(userId).panNumber("ABCDE1234F").build()));
        given(contactDetails.findById(userId)).willReturn(Optional.of(UserContactDetails.builder().userId(userId).city("Delhi").build()));

        var dto = controller.accountSettings(userId, "header@example.com", "Header", "USER");

        assertEquals(dto.getName(), "MANISH");
        assertEquals(dto.getEmail(), "auth@example.com");
        assertEquals(dto.getProvider(), "LOCAL");
        assertEquals(dto.getPanNumber(), "ABCDE1234F");
        assertEquals(dto.getCity(), "Delhi");
    }

    @Test
    void updateAccountSettingsPersistsNormalizedProfileIdentityContactAndUsername() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.builder().userId(userId).build();
        UserIdentityDocument identity = UserIdentityDocument.builder().userId(userId).build();
        UserContactDetails contact = UserContactDetails.builder().userId(userId).build();
        UserAccountSettingsUpdateRequestDto request = new UserAccountSettingsUpdateRequestDto();
        request.setName(" ");
        request.setUsername(" MANISH ");
        request.setDateOfBirth(LocalDate.of(1990, 1, 2));
        request.setAadhaarNumber("1234 5678 9012");
        request.setPanNumber("abcde1234f");
        request.setPhoneNumber(" +91 98765-43210 ");
        request.setAddressLine(" ");
        request.setCity("Delhi");
        request.setState("DL");
        request.setCountry("India");
        request.setPostalCode(" 110001 ");
        given(profiles.findById(userId)).willReturn(Optional.of(profile));
        given(identityDocuments.findById(userId)).willReturn(Optional.of(identity));
        given(contactDetails.findById(userId)).willReturn(Optional.of(contact));
        given(authUsers.account(userId)).willReturn(Optional.empty());

        var dto = controller.updateAccountSettings(userId, "manish@example.com", "Header", "USER", request);

        assertNull(profile.getName());
        assertEquals(profile.getDateOfBirth(), LocalDate.of(1990, 1, 2));
        assertEquals(profile.getAadhaarNumber(), "123456789012");
        assertEquals(profile.getPanNumber(), "ABCDE1234F");
        assertEquals(profile.getPhoneNumber(), "+91 98765-43210");
        assertNull(profile.getAddressLine());
        assertEquals(identity.getAadhaarNumber(), "123456789012");
        assertEquals(identity.getPanNumber(), "ABCDE1234F");
        assertEquals(contact.getPostalCode(), "110001");
        assertEquals(dto.getEmail(), "manish@example.com");
        verify(authUsers).updateUsername(userId, "manish");
        verify(identityDocuments).save(identity);
        verify(contactDetails).save(contact);
    }

    @Test
    void adminEndpointsDelegateToAuthUserService() {
        UUID userId = UUID.randomUUID();
        UserDto dto = UserDto.builder().userId(userId).name("Admin").email("admin@example.com").roles(Set.of(RoleType.ADMIN)).build();
        UserRoleUpdateRequestDto roleRequest = UserRoleUpdateRequestDto.builder().role("ADMIN").build();
        given(authUsers.get(userId)).willReturn(dto);
        given(authUsers.searchPage("admin", "ADMIN", 0, 10)).willReturn(new com.company.platform.commons.api.PagedResponse<>(List.of(dto), 0, 10, 1, 1, true));
        given(authUsers.updateRoles(userId, Set.of(RoleType.ADMIN))).willReturn(dto);

        assertEquals(controller.get(userId), dto);
        assertEquals(controller.search("admin", "ADMIN", 0, 10).content(), List.of(dto));
        assertEquals(controller.updateRole(userId, roleRequest), dto);
        controller.delete(userId);
    }

    @Test
    void preferencesUseExistingOrClientTimezoneAndUpdateWithFallback() {
        UUID userId = UUID.randomUUID();
        given(preferences.findById(userId)).willReturn(Optional.empty(), Optional.of(UserPreference.builder().userId(userId).timezone("Asia/Kolkata").build()));

        assertEquals(controller.preferences(userId, "Asia/Kolkata").get("timezone"), "Asia/Kolkata");
        assertEquals(controller.preferences(userId, "Invalid/Zone").get("timezone"), "Asia/Kolkata");
        Map<String, Object> updated = controller.updatePreferences(userId, UserPreferenceUpdateRequestDto.builder().timezone("Invalid/Zone").build());

        assertEquals(updated.get("timezone"), "UTC");
        verify(preferences).save(any(UserPreference.class));
    }

    @Test
    void preferencesFallbackToUtcWhenStoredAndClientTimezonesAreMissingOrInvalid() {
        UUID userId = UUID.randomUUID();
        given(preferences.findById(userId)).willReturn(Optional.empty());

        assertEquals(controller.preferences(userId, "Invalid/Zone").get("timezone"), "UTC");
        assertEquals(controller.preferences(userId, null).get("timezone"), "UTC");
    }

    @Test
    void avatarAndDemoSeedPersistProfileData() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.builder().userId(userId).build();
        given(profiles.findById(userId)).willReturn(Optional.of(profile));
        given(authUsers.account(userId)).willReturn(Optional.empty());

        UserDto avatar = controller.avatar(userId, "manish@example.com", null, "USER",
                AvatarUpdateRequestDto.builder().avatarUrl(" /avatar.png ").build());
        assertEquals(avatar.getAvatarUrl(), " /avatar.png ");

        given(preferences.findById(userId)).willReturn(Optional.empty());
        UserDto demo = controller.seedDemoData(new DemoUserRequestDto(userId, "manish@example.com", "MANISH", "manish", "/demo.png"));

        assertEquals(demo.getName(), "MANISH");
        assertEquals(profile.getAvatarUrl(), " /avatar.png ");
        verify(profiles, org.mockito.Mockito.atLeastOnce()).save(profile);
        verify(preferences).save(any(UserPreference.class));
    }
}
