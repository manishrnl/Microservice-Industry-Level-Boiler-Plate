package com.company.platform.auth.repository;

import com.company.platform.auth.entity.User;
import com.company.platform.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    List<UserSession> findByUserAndExpiredFalseOrderByLastActiveDescCreatedAtDesc(User user);

    Optional<UserSession> findBySessionIdAndExpiredFalse(String sessionId);

    @Modifying
    @Query("delete from UserSession s where s.user = :user and s.expired = false and s.deviceId = :deviceId")
    int deleteActiveByUserAndDeviceId(@Param("user") User user, @Param("deviceId") String deviceId);

    @Modifying
    @Query("""
            delete from UserSession s
            where s.user = :user
              and s.expired = false
              and s.userAgent = :userAgent
              and (s.deviceId is null or s.deviceId = '')
            """)
    int deleteLegacyBrowserSessionsByUserAndUserAgent(@Param("user") User user, @Param("userAgent") String userAgent);

    @Modifying
    @Query("delete from UserSession s where s.user = :user and s.expired = false and s.ipAddress = :ipAddress")
    int deleteActiveByUserAndIpAddress(@Param("user") User user, @Param("ipAddress") String ipAddress);

    @Modifying
    @Query("delete from UserSession s where s.user = :user")
    int deleteAllByUser(@Param("user") User user);

    @Modifying
    @Query("delete from UserSession s where s.user = :user and s.sessionId = :sessionId")
    int deleteByUserAndSessionId(@Param("user") User user, @Param("sessionId") String sessionId);

    @Modifying
    @Query("update UserSession s set s.lastActive = CURRENT_TIMESTAMP where s.sessionId = :sessionId and s.expired = false")
    int touchSessionBySessionId(@Param("sessionId") String sessionId);
}
