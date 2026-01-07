package com.jobshunter.database.service;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserSessionEntity;
import com.jobshunter.database.repository.UserSessionRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionDBService {

  private final UserSessionRepository userSessionRepository;

  /**
   * Creates a new session or updates an existing one for the given user and device. If a session already exists for this user (enforced by unique
   * constraint on user_id), it will be updated.
   *
   * @param user             The user entity
   * @param deviceId         The device ID (UUID string)
   * @param refreshTokenHash Hashed refresh token
   * @param expiresAt        Token expiration timestamp
   * @param userAgent        User agent string
   * @param ipAddress        IP address
   * @return The created or updated session entity
   */
  @Transactional
  public UserSessionEntity createOrUpdateSession(
      UserEntity user,
      String deviceId,
      String refreshTokenHash,
      Instant expiresAt,
      String userAgent,
      String ipAddress
  ) {
    UserSessionEntity session = userSessionRepository.findByUser(user)
        .orElseGet(() -> {
          log.debug("Created new session for user: {}, device: {}", user.getUsername(), deviceId);
          return new UserSessionEntity();
        });
    session.setDeviceId(deviceId);
    session.setRefreshTokenHash(refreshTokenHash);
    session.setRefreshExpiresAt(expiresAt);
    session.setLastUsedAt(Instant.now());
    session.setUserAgent(userAgent);
    session.setIpAddress(ipAddress);
    session.setRevokedAt(null);
    session.setRevokeReason(null);
    return userSessionRepository.save(session);
  }

  /**
   * Finds a valid session matching user, device, and refresh token hash. A session is valid if: - It matches the user, deviceId, and hash - It is not
   * revoked (revokedAt IS NULL) - It is not expired (refreshExpiresAt > NOW())
   *
   * @param user             The user entity
   * @param deviceId         The device ID (UUID string)
   * @param refreshTokenHash Hashed refresh token
   * @return Optional session entity if found and valid
   */
  public Optional<UserSessionEntity> findValidSession(
      UserEntity user,
      String deviceId,
      String refreshTokenHash
  ) {
    return userSessionRepository.findValidSession(user.getId(), deviceId, refreshTokenHash);
  }

  /**
   * Finds a valid session matching device and refresh token hash (without requiring user). Used for public endpoints like /refresh where we don't
   * have authentication context. A session is valid if: - It matches the deviceId and hash - It is not revoked (revokedAt IS NULL) - It is not
   * expired (refreshExpiresAt > NOW())
   *
   * @param deviceId         The device ID (UUID string)
   * @param refreshTokenHash Hashed refresh token
   * @return Optional session entity if found and valid
   */
  public Optional<UserSessionEntity> findValidSessionByDeviceAndHash(
      String deviceId,
      String refreshTokenHash
  ) {
    return userSessionRepository.findValidSessionByDeviceAndHash(deviceId, refreshTokenHash);
  }

  /**
   * Revokes a specific session for a user and device.
   *
   * @param user     The user entity
   * @param deviceId The device ID (UUID string)
   * @param reason   Reason for revocation
   */
  @Transactional
  public void revokeSession(UserEntity user, String deviceId, String reason) {
    Optional<UserSessionEntity> sessionOpt = userSessionRepository.findByUserAndDeviceId(user, deviceId);
    if (sessionOpt.isPresent()) {
      UserSessionEntity session = sessionOpt.get();
      session.setRevokedAt(Instant.now());
      session.setRevokeReason(reason);
      userSessionRepository.save(session);
      log.debug("Revoked session for user: {}, device: {}, reason: {}", user.getUsername(), deviceId, reason);
    }
  }

  /**
   * Revokes all active sessions for a user.
   *
   * @param user   The user entity
   * @param reason Reason for revocation
   * @return Number of sessions revoked
   */
  @Transactional
  public int revokeAllUserSessions(UserEntity user, String reason) {
    int count = userSessionRepository.revokeAllUserSessions(user, Instant.now(), reason);
    log.info("Revoked {} sessions for user: {}, reason: {}", count, user.getUsername(), reason);
    return count;
  }

  /**
   * Saves a session entity (for updates after token rotation).
   *
   * @param session The session entity to save
   * @return The saved session entity
   */
  @Transactional
  public UserSessionEntity save(UserSessionEntity session) {
    return userSessionRepository.save(session);
  }
}
