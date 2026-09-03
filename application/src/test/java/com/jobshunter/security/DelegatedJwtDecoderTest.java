package com.jobshunter.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.service.OAuth2UserDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.service.application.JwtService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class DelegatedJwtDecoderTest {
  private static RSAKey trustedSigningKey;
  private static RSAKey unknownSigningKey;
  private static ServerSocket jwksServerSocket;
  private static ExecutorService jwksServerExecutor;
  private static volatile boolean jwksServerRunning;
  private static final AtomicReference<String> JWKS_BODY = new AtomicReference<>("{\"keys\":[]}");
  private static int jwksPort;

  @BeforeAll
  static void setUp() throws Exception {
    trustedSigningKey = generateRsaKey("mcp-key-1");
    unknownSigningKey = generateRsaKey("unknown-key");
    jwksPort = pickRandomPort();
    jwksServerSocket = new ServerSocket(jwksPort);
    jwksServerRunning = true;
    jwksServerExecutor = Executors.newSingleThreadExecutor();
    jwksServerExecutor.submit(() -> {
      while (jwksServerRunning) {
        try {
          Socket socket = jwksServerSocket.accept();
          respondWithJwks(socket);
        } catch (Exception ignored) {
          if (!jwksServerRunning) {
            break;
          }
        }
      }
    });
  }

  @AfterAll
  static void tearDown() throws Exception {
    jwksServerRunning = false;
    if (jwksServerSocket != null && !jwksServerSocket.isClosed()) {
      jwksServerSocket.close();
    }
    if (jwksServerExecutor != null) {
      jwksServerExecutor.shutdownNow();
    }
  }

  @BeforeEach
  void setupJwks() throws Exception {
    String jwksJson = new ObjectMapper()
        .writeValueAsString(new JWKSet(trustedSigningKey.toPublicJWK()).toJSONObject());
    JWKS_BODY.set(jwksJson);
  }

  @Test
  void shouldDecodeValidDelegatedToken() throws Exception {
    JwtDecoder decoder = delegatedDecoder("https://mcp.local", "jobshunter-internal-api", "jobshunter_delegated");
    String token = token(trustedSigningKey, "https://mcp.local", "jobshunter-internal-api", "user@example.com", "jobshunter_delegated");

    Jwt decoded = decoder.decode(token);

    assertEquals("user@example.com", decoded.getClaimAsString("email"));
  }

  @Test
  void shouldRejectInvalidIssuer() throws Exception {
    JwtDecoder decoder = delegatedDecoder("https://mcp.local", "jobshunter-internal-api", "jobshunter_delegated");
    String token = token(trustedSigningKey, "https://evil.local", "jobshunter-internal-api", "user@example.com", "jobshunter_delegated");

    assertThrows(JwtException.class, () -> decoder.decode(token));
  }

  @Test
  void shouldRejectInvalidAudience() throws Exception {
    JwtDecoder decoder = delegatedDecoder("https://mcp.local", "jobshunter-internal-api", "jobshunter_delegated");
    String token = token(trustedSigningKey, "https://mcp.local", "wrong-audience", "user@example.com", "jobshunter_delegated");

    assertThrows(JwtException.class, () -> decoder.decode(token));
  }

  @Test
  void shouldRejectUnknownSignatureKey() throws Exception {
    JwtDecoder decoder = delegatedDecoder("https://mcp.local", "jobshunter-internal-api", "jobshunter_delegated");
    String token = token(unknownSigningKey, "https://mcp.local", "jobshunter-internal-api", "user@example.com", "jobshunter_delegated");

    assertThrows(JwtException.class, () -> decoder.decode(token));
  }

  private JwtDecoder delegatedDecoder(String issuer, String audience, String requiredTokenUse) {
    DelegatedAuthProperties delegatedAuthProperties = new DelegatedAuthProperties(
        issuer,
        audience,
        "http://localhost:" + jwksPort + "/.well-known/jwks.json",
        null,
        requiredTokenUse
    );
    SecurityConfig securityConfig = new SecurityConfig(
        mock(UserDBService.class),
        mock(CookieService.class),
        mock(JwtService.class),
        mock(ApplicationProperties.class),
        delegatedAuthProperties,
        mock(OAuth2UserDBService.class),
        mock(OAuth2AuthenticationSuccessHandler.class),
        mock(OAuth2AuthenticationFailureHandler.class)
    );
    return securityConfig.delegatedJwtDecoder();
  }

  private static RSAKey generateRsaKey(String keyId) throws Exception {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
        .privateKey((RSAPrivateKey) keyPair.getPrivate())
        .keyID(keyId)
        .build();
  }

  private static String token(RSAKey key, String issuer, String audience, String email, String tokenUse) throws JOSEException {
    Instant now = Instant.now();
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .subject(email)
        .audience(List.of(audience))
        .claim("email", email)
        .claim("scope", "openid email profile")
        .claim("token_use", tokenUse)
        .issueTime(Date.from(now))
        .notBeforeTime(Date.from(now))
        .expirationTime(Date.from(now.plusSeconds(300)))
        .build();
    SignedJWT signedJwt = new SignedJWT(
        new JWSHeader.Builder(JWSAlgorithm.RS256)
            .type(JOSEObjectType.JWT)
            .keyID(key.getKeyID())
            .build(),
        claimsSet
    );
    signedJwt.sign(new RSASSASigner(key.toPrivateKey()));
    return signedJwt.serialize();
  }

  private static int pickRandomPort() throws Exception {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  private static void respondWithJwks(Socket socket) throws Exception {
    try (socket;
         BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
         OutputStream output = socket.getOutputStream()) {
      String line;
      while ((line = reader.readLine()) != null && !line.isEmpty()) {
        // Consume request headers.
      }
      String body = JWKS_BODY.get();
      byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
      String response = "HTTP/1.1 200 OK\r\n"
          + "Content-Type: application/json\r\n"
          + "Content-Length: " + bodyBytes.length + "\r\n"
          + "Connection: close\r\n"
          + "\r\n";
      output.write(response.getBytes(StandardCharsets.UTF_8));
      output.write(bodyBytes);
      output.flush();
    }
  }
}
