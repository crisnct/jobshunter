package com.jobshunter.security;

import com.jobshunter.database.service.UserDataService;
import com.jobshunter.service.application.authentication.JwtAuthenticationFilter;
import com.jobshunter.service.application.authentication.JwtService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final UserDataService userDataService;
  private final JwtService jwtService;

  @Bean
  public CookieCsrfTokenRepository cookieCsrfTokenRepository() {
    CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repository.setCookiePath("/");
    repository.setCookieName("XSRF-TOKEN");
    repository.setHeaderName("X-XSRF-TOKEN");
    repository.setParameterName("_csrf");
    repository.setCookieCustomizer(cookie -> {
      cookie.path("/");
      cookie.sameSite("Strict");
      cookie.secure(true);
    });
    return repository;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      SecurityHeadersFilter securityHeadersFilter,
      CookieCsrfTokenRepository csrfTokenRepository,
      UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder
  ) {
    // SECURITY FIX: Configure CSRF for stateless JWT authentication
    // Use custom CSRF token repository that works with stateless sessions
    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
    requestHandler.setCsrfRequestAttributeName("_csrf");

    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf
            .csrfTokenRequestHandler(requestHandler)
            .csrfTokenRepository(csrfTokenRepository)
            .ignoringRequestMatchers("/api/auth/**")
            .ignoringRequestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**")
        )
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**").permitAll()
            .requestMatchers("/assets/**", "/src/**", "/favicon.jpg", "/manifest.webmanifest", "/robots.txt").permitAll()
            .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/openapi.yml").permitAll()
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/test/**").hasRole("TEST")
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .anyRequest().authenticated()
        )
        .authenticationProvider(daoAuthenticationProvider(userDetailsService, passwordEncoder))
        .addFilterBefore(securityHeadersFilter, AnonymousAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  private CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public UserDetailsService userDetailsService() {
    return username -> userDataService.getUser(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(UserDetailsService userDetailsService) {
    return new JwtAuthenticationFilter(jwtService, userDetailsService);
  }

  private AuthenticationProvider daoAuthenticationProvider(
      UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
