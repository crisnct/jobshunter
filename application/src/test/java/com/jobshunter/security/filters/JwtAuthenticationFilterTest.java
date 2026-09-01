package com.jobshunter.security.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.jobshunter.service.application.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

class JwtAuthenticationFilterTest {

  private JwtService jwtService;
  private UserDetailsService userDetailsService;
  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    jwtService = mock(JwtService.class);
    userDetailsService = mock(UserDetailsService.class);
    filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldBypassInternalApiPath() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/internal/search_jobs");
    request.setMethod("POST");
    request.addHeader("Authorization", "Bearer eyJ.mock");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertEquals(200, response.getStatus());
    verifyNoInteractions(jwtService, userDetailsService);
  }
}
