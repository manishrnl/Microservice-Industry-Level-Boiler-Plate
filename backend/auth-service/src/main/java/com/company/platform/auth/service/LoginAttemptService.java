package com.company.platform.auth.service;

import com.company.platform.auth.entity.User;
import com.company.platform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private static final String ACCOUNT_STATUS_ACTIVE = "ACTIVE";

    private final UserRepository userRepository;

    @Value("${app.security.login.max-failed-attempts:10}")
    private int maxFailedAttempts;

    @Value("${app.security.login.lock-duration-minutes:30}")
    private long lockDurationMinutes;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LoginFailureResult recordFailure(UUID userId) {
        User user = requireUserForUpdate(userId);
        int attempts = Math.min(Math.max(1, maxFailedAttempts), user.getFailedAttempts() + 1);
        user.setFailedAttempts(attempts);
        if (attempts >= Math.max(1, maxFailedAttempts)) {
            user.setAccountLocked(true);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(Math.max(1, lockDurationMinutes)));
        }
        userRepository.saveAndFlush(user);
        return new LoginFailureResult(attempts, user.isAccountLocked(), user.getLockedUntil());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UUID userId) {
        User user = requireUserForUpdate(userId);
        if (user.getFailedAttempts() == 0 && !isExpiredLoginLock(user)) {
            return;
        }
        user.setFailedAttempts(0);
        if (isActive(user)) {
            user.setAccountLocked(false);
            user.setLockedUntil(null);
        }
        userRepository.saveAndFlush(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean unlockIfExpired(UUID userId) {
        User user = requireUserForUpdate(userId);
        if (!isExpiredLoginLock(user)) {
            return false;
        }
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        user.setLockedUntil(null);
        userRepository.saveAndFlush(user);
        return true;
    }

    private User requireUserForUpdate(UUID userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("User disappeared while updating login attempts"));
    }

    private boolean isExpiredLoginLock(User user) {
        return user.isAccountLocked()
                && isActive(user)
                && user.getLockedUntil() != null
                && !user.getLockedUntil().isAfter(LocalDateTime.now());
    }

    private boolean isActive(User user) {
        return user.getAccountStatus() == null
                || user.getAccountStatus().isBlank()
                || ACCOUNT_STATUS_ACTIVE.equals(user.getAccountStatus());
    }

    public int maxFailedAttempts() {
        return Math.max(1, maxFailedAttempts);
    }

    public record LoginFailureResult(int failedAttempts, boolean locked, LocalDateTime lockedUntil) {
    }
}
