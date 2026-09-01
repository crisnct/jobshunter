package com.jobshunter.security.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.jobshunter.database.service.UserDBService;
import com.jobshunter.security.CookieService;
import com.jobshunter.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class DeviceIdFilterTest {

  private DeviceIdFilter deviceIdFilter;

  @BeforeEach
  void setUp() {
    deviceIdFilter = new DeviceIdFilter(
        mock(UserDBService.class),
        mock(CookieService.class),
        new RestAuthenticationEntryPoint()
    );
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldBypassDeviceCheckForInternalApiPath() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.setRequestURI("/api/internal/search_jobs");
    MockHttpServletResponse response = new MockHttpServletResponse();

    deviceIdFilter.doFilter(request, response, new MockFilterChain());

    assertEquals(200, response.getStatus());
  }
}
