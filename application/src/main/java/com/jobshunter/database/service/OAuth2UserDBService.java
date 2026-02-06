package com.jobshunter.database.service;

import com.jobshunter.database.entities.RoleEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserOAuthProviderEntity;
import com.jobshunter.database.repository.RoleRepository;
import com.jobshunter.database.repository.UserOAuthProviderRepository;
import com.jobshunter.database.repository.UserRepository;
import com.jobshunter.dto.exceptions.BusinessException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that processes OAuth2 user information from Google (and potentially other providers).
 * Handles user registration, linking OAuth providers to existing accounts, and user lookup.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserDBService extends DefaultOAuth2UserService {

  private static final String PROVIDER_GOOGLE = "google";

  private final UserRepository userRepository;
  private final UserOAuthProviderRepository oauthProviderRepository;
  private final RoleRepository roleRepository;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(userRequest);
    String registrationId = userRequest.getClientRegistration().getRegistrationId();

    return processOAuth2User(registrationId, oAuth2User);
  }

  /**
   * Process OAuth2 user: find existing user, create new user, or link OAuth to existing account.
   */
  private OAuth2User processOAuth2User(String provider, OAuth2User oAuth2User) {
    Map<String, Object> attributes = oAuth2User.getAttributes();

    String providerId = extractProviderId(provider, attributes);
    String email = extractEmail(attributes);
    String name = extractName(attributes);

    log.debug("Processing OAuth2 user: provider={}, providerId={}, email={}", provider, providerId, email);

    // Check if OAuth provider link already exists
    Optional<UserOAuthProviderEntity> existingOAuthLink =
        oauthProviderRepository.findByProviderAndProviderId(provider, providerId);

    if (existingOAuthLink.isPresent()) {
      // User already has this OAuth provider linked - return existing user
      UserEntity user = existingOAuthLink.get().getUser();
      log.debug("Found existing OAuth link for user: {}", user.getUsername());
      return oAuth2User;
    }

    // Check if a user with this email already exists
    Optional<UserEntity> existingUser = userRepository.findByEmail(email);

    if (existingUser.isPresent()) {
      // Auto-link OAuth provider to existing user
      UserEntity user = existingUser.get();
      linkOAuthProvider(user, provider, providerId, email);
      log.info("Linked {} OAuth to existing user: {}", provider, user.getUsername());
      return oAuth2User;
    }

    // Create new user
    UserEntity newUser = createOAuthUser(provider, providerId, email, name);
    log.info("Created new user via {} OAuth: {}", provider, newUser.getUsername());
    return oAuth2User;
  }

  /**
   * Create a new user from OAuth2 information.
   */
  private UserEntity createOAuthUser(String provider, String providerId, String email, String name) {
    RoleEntity userRole = roleRepository.findByName("USER")
        .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Default role USER not configured"));

    // Generate a unique username from email prefix + random suffix
    String baseUsername = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
    String username = generateUniqueUsername(baseUsername);

    UserEntity user = new UserEntity();
    user.setUsername(username);
    user.setEmail(email);
    user.setPhoneNumber(""); // OAuth users don't provide phone during signup
    user.setPassword(UUID.randomUUID().toString()); // Random password, not usable for login
    user.setEmailVerified(true); // Google already verified the email
    user.setApproved(true); // Auto-approve OAuth users
    user.setCreatedAt(Instant.now());
    user.setNotifyEmail(true);
    user.getRoles().add(userRole);

    user = userRepository.save(user);

    // Link the OAuth provider to the new user
    linkOAuthProvider(user, provider, providerId, email);

    return user;
  }

  /**
   * Link an OAuth provider to an existing user.
   */
  private void linkOAuthProvider(UserEntity user, String provider, String providerId, String email) {
    UserOAuthProviderEntity oauthProvider = new UserOAuthProviderEntity(user, provider, providerId, email);
    oauthProviderRepository.save(oauthProvider);
    user.getOauthProviders().add(oauthProvider);
  }

  /**
   * Generate a unique username by appending random numbers if needed.
   */
  private String generateUniqueUsername(String baseUsername) {
    // Clean the base username
    String cleanUsername = baseUsername.replaceAll("[^a-zA-Z0-9_]", "");
    if (cleanUsername.length() > 40) {
      cleanUsername = cleanUsername.substring(0, 40);
    }
    if (cleanUsername.isEmpty()) {
      cleanUsername = "user";
    }

    // Check if username exists
    if (!userRepository.existsByUsernameIgnoreCase(cleanUsername)) {
      return cleanUsername;
    }

    // Append random numbers until unique
    for (int i = 0; i < 100; i++) {
      String candidateUsername = cleanUsername + "_" + (int) (Math.random() * 10000);
      if (candidateUsername.length() <= 50 && !userRepository.existsByUsernameIgnoreCase(candidateUsername)) {
        return candidateUsername;
      }
    }

    // Fallback to UUID-based username
    return cleanUsername + "_" + UUID.randomUUID().toString().substring(0, 8);
  }

  /**
   * Extract the provider's unique user ID from OAuth2 attributes.
   */
  private String extractProviderId(String provider, Map<String, Object> attributes) {
    if (PROVIDER_GOOGLE.equals(provider)) {
      Object sub = attributes.get("sub");
      if (sub != null) {
        return sub.toString();
      }
    }
    throw new OAuth2AuthenticationException(
        new OAuth2Error("invalid_user", "Could not extract provider ID from OAuth2 user info", null));
  }

  /**
   * Extract email from OAuth2 attributes.
   */
  private String extractEmail(Map<String, Object> attributes) {
    Object email = attributes.get("email");
    if (email != null && !email.toString().isEmpty()) {
      return email.toString();
    }
    throw new OAuth2AuthenticationException(
        new OAuth2Error("invalid_user", "Email is required for OAuth2 authentication", null));
  }

  /**
   * Extract display name from OAuth2 attributes.
   */
  private String extractName(Map<String, Object> attributes) {
    Object name = attributes.get("name");
    return name != null ? name.toString() : "";
  }

  /**
   * Find a user by their OAuth provider and provider ID.
   */
  public Optional<UserEntity> findUserByOAuthProvider(String provider, String providerId) {
    return oauthProviderRepository.findByProviderAndProviderId(provider, providerId)
        .map(UserOAuthProviderEntity::getUser);
  }

  /**
   * Find a user by email from OAuth2 attributes.
   */
  public Optional<UserEntity> findUserByEmail(String email) {
    return userRepository.findByEmail(email);
  }
}
