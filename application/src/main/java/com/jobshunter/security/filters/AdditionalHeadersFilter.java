package com.jobshunter.security.filters;

import com.jobshunter.security.ClientIpResolver;
import com.jobshunter.security.JHHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class AdditionalHeadersFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String ip = ClientIpResolver.resolveClientIp(request);
    response.setHeader(JHHeaders.X_REAL_IP, ip);

    filterChain.doFilter(request, response);
  }

}
