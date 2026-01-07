package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserSessionEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSessionEntity, Long> {

  @Query(
      value = """
            SELECT *
            FROM user_sessions
            WHERE user_id = :userId
              AND device_id = :deviceId
              AND refresh_token_hash = :hash
              AND revoked_at IS NULL
              AND refresh_expires_at > UTC_TIMESTAMP()
          """,
      nativeQuery = true
  )
  Optional<UserSessionEntity> findValidSession(
      @Param("userId") Long userId,
      @Param("deviceId") String deviceId,
      @Param("hash") String refreshTokenHash
  );

  @Query(
      value = """
            SELECT us.*
            FROM user_sessions us
            WHERE us.device_id = :deviceId
              AND us.refresh_token_hash = :hash
              AND us.revoked_at IS NULL
              AND us.refresh_expires_at > UTC_TIMESTAMP()
            LIMIT 1
          """,
      nativeQuery = true
  )
  Optional<UserSessionEntity> findValidSessionByDeviceAndHash(
      @Param("deviceId") String deviceId,
      @Param("hash") String refreshTokenHash
  );

  Optional<UserSessionEntity> findByUserAndDeviceId(UserEntity user, String deviceId);

  Optional<UserSessionEntity> findByUser(UserEntity user);

  @Modifying
  @Query("UPDATE UserSessionEntity us SET us.revokedAt = :revokedAt, us.revokeReason = :reason " +
      "WHERE us.user = :user AND us.revokedAt IS NULL")
  int revokeAllUserSessions(@Param("user") UserEntity user,
      @Param("revokedAt") Instant revokedAt,
      @Param("reason") String reason);

  @Modifying
  @Query("DELETE FROM UserSessionEntity us " +
      "WHERE us.refreshExpiresAt < :now OR us.revokedAt IS NOT NULL")
  int deleteExpiredOrRevokedSessions(@Param("now") Instant now);
}
