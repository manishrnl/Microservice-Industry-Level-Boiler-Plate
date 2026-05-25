package com.company.platform.commons.dto;

import com.company.platform.commons.enums.AuditAction;
import com.company.platform.commons.enums.EventStatus;
import com.company.platform.commons.enums.NotificationCategory;
import com.company.platform.commons.enums.NotificationType;
import com.company.platform.commons.enums.RoleType;
import com.company.platform.commons.enums.TokenRevocationReason;
import com.company.platform.commons.event.Events;
import org.testng.annotations.Test;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.*;

class CommonDtoAndEventContractsTest {

    @Test
    void recordDtosExposeConstructorValues() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        PermissionDto permission = new PermissionDto(UUID.randomUUID(), "users.read", "users", "read");

        assertEquals(new AuditEventDto(userId, "admin", AuditAction.UPDATE, "user", resourceId.toString(),
                "127.0.0.1", "Chrome", "trace-1", Map.of("before", "old"), Map.of("after", "new"),
                "COMPLETED", null, now).traceId(), "trace-1");
        assertEquals(new ChatMessageDto(resourceId, UUID.randomUUID(), "user", "hello", 1, "model", now).content(), "hello");
        assertEquals(new ChatSessionDto(resourceId, userId, "Support", "model", 42, false, now, now).totalTokens(), 42);
        assertEquals(new DemoUserRequestDto(userId, "u@example.com", "User", "user", "/avatar.png").username(), "user");
        assertEquals(new FileMetadataDto(resourceId, userId, "file.txt", "text/plain", 12, true, now).sizeBytes(), 12);
        assertEquals(new NotificationDto(resourceId, NotificationCategory.SYSTEM, "Title", "Message", "/app", false, now).category(), NotificationCategory.SYSTEM);
        assertEquals(new NotificationEventDto(userId, NotificationType.IN_APP, NotificationCategory.ALERT, "Title", "Message", "/app").type(), NotificationType.IN_APP);
        assertEquals(new PagedResponseDto<>(List.of("a"), 0, 1, 1, 1, true).content(), List.of("a"));
        assertEquals(permission.action(), "read");
        assertEquals(new RoleDto(UUID.randomUUID(), RoleType.ADMIN, Set.of(permission)).permissions(), Set.of(permission));
    }

    @Test
    void lombokDtosSupportBuilderNoArgsAndMutators() {
        TokenDto token = TokenDto.builder()
                .accessToken("access")
                .tokenType("Bearer")
                .expiresInSeconds(900)
                .build();
        UserDto user = new UserDto();
        UUID userId = UUID.randomUUID();

        user.setUserId(userId);
        user.setName("User");
        user.setEmail("u@example.com");
        user.setUsername("user");
        user.setRoles(Set.of(RoleType.USER));
        user.setAvatarUrl("/avatar.png");

        assertEquals(token.getAccessToken(), "access");
        assertEquals(token.getTokenType(), "Bearer");
        assertEquals(token.getExpiresInSeconds(), 900);
        assertEquals(user.getUserId(), userId);
        assertEquals(user.getUsername(), "user");
        assertEquals(user.getRoles(), Set.of(RoleType.USER));
        assertTrue(user.toString().contains("u@example.com"));
    }

    @Test
    void eventRecordsAndEnumsExposeStableContracts() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        assertEquals(new Events.UserRegisteredEvent(userId, "u@example.com", "User", now).email(), "u@example.com");
        assertEquals(new Events.UserLoginEvent(userId, "u@example.com", "session", "127.0.0.1", now).sessionId(), "session");
        assertEquals(new Events.NewDeviceLoginEvent(userId, "u@example.com", "device", "127.0.0.1", "Chrome").deviceId(), "device");
        assertEquals(new Events.PasswordChangedEvent(userId, "u@example.com", now).changedAt(), now);
        assertEquals(new Events.PaymentInitiatedEvent(paymentId, userId, BigDecimal.TEN, "INR").amount(), BigDecimal.TEN);
        assertEquals(new Events.PaymentCompletedEvent(paymentId, userId, BigDecimal.ONE, "USD").currency(), "USD");
        assertEquals(new Events.PaymentFailedEvent(paymentId, userId, BigDecimal.ONE, "USD", "declined").reason(), "declined");
        assertEquals(new Events.AuditEvent(userId, "admin", AuditAction.DELETE, "user", "u1", "trace",
                Map.of("before", true), Map.of("after", false), EventStatus.COMPLETED).action(), AuditAction.DELETE);
        assertEquals(new Events.NotificationEvent(userId, NotificationType.EMAIL, NotificationCategory.AUTH,
                "Title", "Message", "/login").category(), NotificationCategory.AUTH);

        assertTrue(List.of(AuditAction.values()).contains(AuditAction.ROLE_CHANGE));
        assertEquals(EventStatus.valueOf("FAILED"), EventStatus.FAILED);
        assertEquals(NotificationCategory.values().length, 4);
        assertEquals(NotificationType.valueOf("PUSH"), NotificationType.PUSH);
        assertEquals(RoleType.valueOf("SUPER_ADMIN"), RoleType.SUPER_ADMIN);
        assertEquals(TokenRevocationReason.valueOf("PASSWORD_CHANGE"), TokenRevocationReason.PASSWORD_CHANGE);

        Constructor<Events> constructor = Events.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }
}
