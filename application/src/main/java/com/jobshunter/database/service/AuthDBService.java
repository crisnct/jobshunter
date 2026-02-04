package com.jobshunter.database.service;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.entities.RoleEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserSessionEntity;
import com.jobshunter.database.repository.RoleRepository;
import com.jobshunter.database.repository.UserRepository;
import com.jobshunter.dto.ChangePasswordRequest;
import com.jobshunter.dto.IpInfoDetailResponse;
import com.jobshunter.dto.LoginRequest;
import com.jobshunter.dto.LoginResult;
import com.jobshunter.dto.RegisterRequest;
import com.jobshunter.dto.exceptions.BusinessException;
import com.jobshunter.dto.exceptions.ValidationException;
import com.jobshunter.service.application.JwtService;
import com.jobshunter.service.application.RefreshTokenService;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.clients.IpInfo;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthDBService {

  private final UserRepository userRepository;

  private final RoleRepository roleRepository;

  private final PasswordEncoder passwordEncoder;

  private final JwtService jwtService;

  private final RefreshTokenService refreshTokenService;

  private final UserSessionDBService userSessionDBService;

  private final ApplicationProperties properties;

  private final AuthenticationManager authenticationManager;

  private final EmailNotifierService emailService;

  private final IpInfo ipInfo;

  @Transactional
  public UserEntity register(RegisterRequest request) {
    if (userRepository.existsByUsernameIgnoreCase(request.username())) {
      throw new ValidationException("Username already in use");
    }
    if (userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new ValidationException("Email already in use");
    }
    if (userRepository.existsByPhoneNumberIgnoreCase(request.phoneNumber())) {
      throw new ValidationException("Phone number already in use");
    }

    RoleEntity userRole = roleRepository.findByName("USER")
        .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Default role USER not configured"));

    UserEntity user = new UserEntity();
    user.setUsername(request.username());
    user.setEmail(request.email());
    user.setPhoneNumber(request.phoneNumber());
    user.setPassword(passwordEncoder.encode(request.password()));
    user.setEmailVerified(false);
    user.setCreatedAt(Instant.now());
    user.setNotifyEmail(true);
    user.setVerificationToken(UUID.randomUUID().toString());
    user.getRoles().add(userRole);

    return userRepository.save(user);
  }

  @Transactional
  public LoginResult login(LoginRequest request, String deviceId, String userAgent, String ipAddress) {
    //Validations
    UserEntity user = userRepository.findByUsername(request.username())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    if (!user.isEmailVerified()) {
      throw new ValidationException("Email not verified");
    }
    if (!user.isApproved()) {
      throw new ValidationException("Account was not approved yet. Approval process might takes 72h!");
    }

    //Authentication
    try {
      authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
    } catch (BadCredentialsException ex) {
      throw new ValidationException("Invalid credentials");
    }

    // Generate refresh token and hash it
    String refreshToken = refreshTokenService.generateRefreshToken();
    String refreshTokenHash = refreshTokenService.hashRefreshToken(refreshToken);
    int expiration = properties.getSecurity().getRefreshToken().getExpirationSec();
    Instant expiresAt = Instant.now().plusSeconds(expiration);

    // Create or update session (unique constraint on user_id ensures one device per user)
    UserSessionEntity session = userSessionDBService.createOrUpdateSession(user, deviceId, refreshTokenHash, expiresAt, userAgent, ipAddress);

    // Update session with IP location information
    try {
      IpInfoDetailResponse ipInfoDetail = ipInfo.getIpDetailInfo(ipAddress);
      if (ipInfoDetail != null) {
        userSessionDBService.updateSessionWithIpInfo(session, ipInfoDetail);
      }
    } catch (Exception e) {
      log.warn("Failed to fetch IP location information for IP {}: {}", ipAddress, e.getMessage(), e);
    }

    // Generate access token with session ID
    String jwtToken = jwtService.generateToken(user, session.getId());

    return new LoginResult(jwtToken, refreshToken);
  }

  @Transactional
  public void verifyEmail(String token) {
    UserEntity user = userRepository.findByVerificationToken(token)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification token"));
    user.setEmailVerified(true);
    user.setVerificationToken(null);
    userRepository.save(user);
    List<String> adminEmails = userRepository.findEmailsByRole("ADMIN");
    emailService.sendMailToApproveAccount(user, adminEmails);
  }

  @Transactional
  public UserEntity changePassword(String username, ChangePasswordRequest request) {
    UserEntity user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username"));
    if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
      throw new ValidationException("Password doesn't match");
    }
    user.setPassword(passwordEncoder.encode(request.newPassword()));
    user.setEmailVerified(false);
    user.setVerificationToken(UUID.randomUUID().toString());
    return userRepository.save(user);
  }

}
