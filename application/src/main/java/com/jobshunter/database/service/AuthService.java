package com.jobshunter.database.service;

import com.jobshunter.database.entities.RoleEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.repository.RoleRepository;
import com.jobshunter.database.repository.UserRepository;
import com.jobshunter.dto.ChangePasswordRequest;
import com.jobshunter.dto.LoginRequest;
import com.jobshunter.dto.RegisterRequest;
import com.jobshunter.service.application.authentication.JwtService;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class AuthService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  @Autowired
  private AuthenticationManager authenticationManager;

  @Transactional
  public UserEntity register(RegisterRequest request) {
    if (userRepository.existsByUsernameIgnoreCase(request.username())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already in use");
    }
    if (userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already in use");
    }
    if (userRepository.existsByPhoneNumberIgnoreCase(request.phoneNumber())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number already in use");
    }

    RoleEntity userRole = roleRepository.findByName("USER")
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Default role USER not configured"));

    UserEntity user = new UserEntity();
    user.setUsername(request.username());
    user.setEmail(request.email());
    user.setPhoneNumber(request.phoneNumber());
    user.setPassword(passwordEncoder.encode(request.password()));
    user.setEmailVerified(false);
    user.setCreatedAt(LocalDateTime.now());
    user.setNotifyEmail(true);
    user.setTimeInterval((int) TimeUnit.DAYS.toMinutes(1));
    user.setVerificationToken(UUID.randomUUID().toString());
    user.getRoles().add(userRole);

    return userRepository.save(user);
  }

  public String login(LoginRequest request) {
    UserEntity user = userRepository.findByUsername(request.username())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

    if (!user.isEmailVerified()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Email not verified");
    }

    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.username(), request.password()));
    } catch (BadCredentialsException ex) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    return jwtService.generateToken(user);
  }

  @Transactional
  public void verifyEmail(String token) {
    UserEntity user = userRepository.findByVerificationToken(token)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification token"));

    user.setEmailVerified(true);
    user.setVerificationToken(null);
    userRepository.save(user);
  }

  @Transactional
  public UserEntity changePassword(String username, ChangePasswordRequest request) {
    UserEntity user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username"));
    if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password doesn't match");
    }
    user.setPassword(passwordEncoder.encode(request.newPassword()));
    user.setEmailVerified(false);
    user.setVerificationToken(UUID.randomUUID().toString());
    return userRepository.save(user);
  }

}
