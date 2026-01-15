package com.jobshunter.security.filters;

import com.jobshunter.security.JHHeaders;
import com.jobshunter.service.application.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;

  private final UserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String authHeader = request.getHeader(JHHeaders.AUTHORIZATION);
    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    log.debug("JWT Filter - URI: {}, Processing JWT token", request.getRequestURI());
    String token = authHeader.substring(BEARER_PREFIX.length());
    String username;
    try {
      username = jwtService.extractUsername(token);
    } catch (Exception ex) {
      log.debug("Failed to parse JWT: {}", ex.getMessage());
      filterChain.doFilter(request, response);
      return;
    }

    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      if (jwtService.isTokenValid(token)) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Extract session ID for audit (optional)
        Long sessionId = jwtService.extractSessionId(token);
        if (sessionId != null) {
          log.debug("JWT Filter - Session ID: {}", sessionId);
        }
      } else {
        log.debug("JWT Filter - Invalid or expired token for user: {}", username);
      }
    }

    filterChain.doFilter(request, response);
  }
}
