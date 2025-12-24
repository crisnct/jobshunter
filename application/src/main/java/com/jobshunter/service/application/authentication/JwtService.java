package com.jobshunter.service.application.authentication;

import com.jobshunter.database.entities.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService {

  private final JwtProperties jwtProperties;

  public JwtService(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  public String extractUsername(String token) {
    return extractAllClaims(token).getSubject();
  }

  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = Map.of(
        "roles",
        userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList()
    );
    Date now = new Date();
    Date expiry = new Date(now.getTime() + jwtProperties.getExpirationMs());

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(userDetails.getUsername())
        .setIssuedAt(now)
        .setExpiration(expiry)
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public boolean isTokenValid(String token, UserEntity user) {
    try {
      String hashedToken = hashToken(token);
      if (user.getJwtToken() == null || !user.getJwtToken().equals(hashedToken)) {
        return false;
      }
      String username = extractUsername(token);
      return username.equals(user.getUsername()) && !isTokenExpired(token);
    } catch (Exception ex) {
      return false;
    }
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(getSigningKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  private boolean isTokenExpired(String token) {
    return extractAllClaims(token).getExpiration().before(new Date());
  }

  private Key getSigningKey() {
    if (!StringUtils.hasText(jwtProperties.getSecret())) {
      throw new IllegalStateException("JWT secret (security.jwt.secret) must be configured");
    }
    if (jwtProperties.getExpirationMs() <= 0) {
      throw new IllegalStateException("JWT expiration (security.jwt.expiration-ms) must be positive");
    }
    return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String hashToken(String token) {
    MessageDigest digest = getDigest();
    byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(hash);
  }

  private MessageDigest getDigest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to initialize SHA-256 MessageDigest", ex);
    }
  }
}
