package com.jobshunter.service.application;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.ApplicationProperties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@AllArgsConstructor
public class JwtService {

  private final ApplicationProperties properties;

  public String extractUsername(String token) {
    return extractAllClaims(token).getSubject();
  }

  public String generateToken(UserDetails userDetails) {
    return generateToken(userDetails, null);
  }

  public String generateToken(UserDetails userDetails, Long sessionId) {
    Map<String, Object> claimsMap = new java.util.HashMap<>();
    claimsMap.put("roles",
        userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList());
    if (sessionId != null) {
      claimsMap.put("sid", sessionId);
    }

    Date now = new Date();

    Date expiry = new Date(now.getTime() + 1000 * properties.getSecurity().getJwt().getExpirationSec());

    return Jwts.builder()
        .setClaims(claimsMap)
        .setSubject(userDetails.getUsername())
        .setIssuedAt(now)
        .setExpiration(expiry)
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public Long extractSessionId(String token) {
    try {
      Claims claims = extractAllClaims(token);
      Object sid = claims.get("sid");
      if (sid instanceof Number) {
        return ((Number) sid).longValue();
      }
      return null;
    } catch (Exception ex) {
      return null;
    }
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(getSigningKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  public boolean isTokenExpired(String token) {
    try {
      return extractAllClaims(token).getExpiration().before(new Date());
    } catch (Exception ex) {
      return true;
    }
  }

  public boolean isTokenValid(String token) {
    try {
      String username = extractUsername(token);
      return username != null && !isTokenExpired(token);
    } catch (Exception ex) {
      return false;
    }
  }

  private Key getSigningKey() {
    JwtProperties jwtProps = properties.getSecurity().getJwt();
    if (!StringUtils.hasText(jwtProps.getSecret())) {
      throw new IllegalStateException("JWT secret (security.jwt.secret) must be configured");
    }
    if (jwtProps.getExpirationSec() <= 0 || jwtProps.getExpirationSec() > 3600) {
      throw new IllegalStateException("JWT expiration (security.jwt.expiration-ms) invalid");
    }
    return Keys.hmacShaKeyFor(jwtProps.getSecret().getBytes(StandardCharsets.UTF_8));
  }

}
