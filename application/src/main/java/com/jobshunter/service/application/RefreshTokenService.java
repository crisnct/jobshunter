package com.jobshunter.service.application;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserSessionEntity;
import com.jobshunter.database.service.UserSessionDBService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private static final int TOKEN_BYTES = 32;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final ApplicationProperties properties;
  private final UserSessionDBService userSessionDBService;

  /**
   * Generates a cryptographically secure refresh token. Uses 32 bytes of SecureRandom, encoded as Base64 URL-safe without padding.
   *
   * @return Base64 URL-safe encoded refresh token
   */
  public String generateRefreshToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /**
   * Hashes a refresh token using SHA-256 with a server-side pepper. Format: SHA-256(token + pepper)
   *
   * @param refreshToken The plain refresh token
   * @return Hex-encoded SHA-256 hash
   */
  public String hashRefreshToken(String refreshToken) {
    String pepper = getPepper();
    String combined = refreshToken + pepper;
    MessageDigest digest = getDigest();
    byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(hash);
  }

  /**
   * Validates a refresh token and performs rotation (generates new token). This implements secure token rotation: every refresh generates a new
   * token.
   *
   * @param user         The user entity
   * @param deviceId     The device ID (UUID string)
   * @param refreshToken The refresh token from client
   * @return The new refresh token (to be sent to client)
   * @throws IllegalArgumentException if token is invalid, expired, or revoked
   */
  @Transactional
  public String validateAndRotate(UserEntity user, String deviceId, String refreshToken) {
    String hash = hashRefreshToken(refreshToken);
    UserSessionEntity session = userSessionDBService.findValidSession(user, deviceId, hash)
        .orElseThrow(() -> {
          log.warn("Invalid refresh token attempt for user: {}, device: {}", user.getUsername(), deviceId);
          return new IllegalArgumentException("Invalid or expired refresh token");
        });

    // Token rotation: generate new refresh token
    String newRefreshToken = generateRefreshToken();
    String newHash = hashRefreshToken(newRefreshToken);

    // Update session with new token hash and expiration
    Instant now = Instant.now();
    int expirationSec = properties.getSecurity().getRefreshToken().getExpirationSec();
    Instant newExpiresAt = Instant.now().plusSeconds(expirationSec);

    session.setRefreshTokenHash(newHash);
    session.setRefreshExpiresAt(newExpiresAt);
    session.setLastUsedAt(now);
    userSessionDBService.save(session);

    log.debug("Refresh token rotated for user: {}, device: {}", user.getUsername(), deviceId);
    return newRefreshToken;
  }

  /**
   * Validates a refresh token and returns the associated user entity. This method is used for public endpoints like /refresh where we don't have
   * authentication context. The user entity is eagerly loaded via JOIN FETCH to avoid LazyInitializationException.
   *
   * @param deviceId     The device ID (UUID string)
   * @param refreshToken The refresh token from client
   * @return The user entity associated with the valid session
   * @throws IllegalArgumentException if token is invalid, expired, or revoked
   */
  @Transactional(readOnly = true)
  public UserEntity validateAndGetUser(String deviceId, String refreshToken) {
    String hash = hashRefreshToken(refreshToken);
    UserSessionEntity session = userSessionDBService.findValidSessionByDeviceAndHash(deviceId, hash)
        .orElseThrow(() -> {
          log.warn("Invalid refresh token attempt for device: {}", deviceId);
          return new IllegalArgumentException("Invalid or expired refresh token");
        });
    Hibernate.initialize(session.getUser());
    return session.getUser();
  }

  private String getPepper() {
    String pepper = properties.getSecurity().getRefreshToken().getPepper();
    if (pepper == null || pepper.isBlank()) {
      throw new IllegalStateException("Refresh token pepper (security.refresh-token.pepper) must be configured");
    }
    return pepper;
  }

  private MessageDigest getDigest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to initialize SHA-256 MessageDigest", ex);
    }
  }
}
