package com.jobshunter.security.filters;

import com.jobshunter.database.service.UserDBService;
import com.jobshunter.dto.exceptions.DeviceRevokedException;
import com.jobshunter.security.CookieService;
import com.jobshunter.security.RestAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

@AllArgsConstructor
public class DeviceIdFilter extends OncePerRequestFilter {

  private final UserDBService userDBService;

  private final CookieService cookieService;

  private final RestAuthenticationEntryPoint authenticationEntryPoint;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      filterChain.doFilter(request, response);
      return;
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof UserDetails userDetails) {
      String username = userDetails.getUsername();

      // Skip device ID check for login endpoint
      String requestPath = request.getRequestURI();
      if (!requestPath.equals("/api/auth/login")) {
        Optional<String> deviceIdCookieOp = cookieService.getCookie(request, CookieService.DEVICE_ID_COOKIE);
        if (deviceIdCookieOp.isEmpty()) {
          this.logout(request, response);
          DeviceRevokedException exception = new DeviceRevokedException("From security reason once in a while you have to re-login again.");
          authenticationEntryPoint.commence(request, response, exception);
          return;
        }
        Optional<String> activeDeviceIdOp = userDBService.getActiveDeviceId(username);
        if (activeDeviceIdOp.isPresent() && !deviceIdCookieOp.get().equals(activeDeviceIdOp.get())) {
          this.logout(request, response);
          DeviceRevokedException exception = new DeviceRevokedException("Logged out because you signed in on another device.");
          authenticationEntryPoint.commence(request, response, exception);
          return;
        }
      }
    }

    filterChain.doFilter(request, response);
  }

  private void logout(HttpServletRequest request, HttpServletResponse response) {
    SecurityContextHolder.clearContext();
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    cookieService.expireCookies(response);
  }

}
