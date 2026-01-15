package com.jobshunter.security;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.security.filters.AdditionalHeadersFilter;
import com.jobshunter.security.filters.DeviceIdFilter;
import com.jobshunter.security.filters.JwtAuthenticationFilter;
import com.jobshunter.security.filters.RateLimitingFilter;
import com.jobshunter.security.filters.SecurityHeadersFilter;
import com.jobshunter.security.rateLimitBucket4J.BlockRegistry;
import com.jobshunter.security.rateLimitBucket4J.InMemoryRateLimiter;
import com.jobshunter.security.rateLimitBucket4J.ViolationRegistry;
import com.jobshunter.service.application.JwtService;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

  public static final long MAX_AGE_CORS = (int) TimeUnit.HOURS.toSeconds(1);
  private static final int MAX_AGE_HSTS = (int) TimeUnit.DAYS.toSeconds(30);

  private final UserDBService userDBService;
  private final CookieService cookieService;
  private final JwtService jwtService;

  @Bean
  public CookieCsrfTokenRepository cookieCsrfTokenRepository() {
    CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repository.setCookiePath("/");
    repository.setCookieName("XSRF-TOKEN");
    repository.setHeaderName(JHHeaders.X_XSRF_TOKEN);
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
      AdditionalHeadersFilter additionalHeadersFilter,
      DeviceIdFilter deviceIdFilter,
      RateLimitingFilter rateLimitingFilter,
      RestAuthenticationEntryPoint restAuthenticationEntryPoint,
      CookieCsrfTokenRepository csrfTokenRepository,
      UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder
  ) {
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
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .anyRequest().authenticated()
        )
        .headers(h ->
            h.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(MAX_AGE_HSTS))
        )
        .authenticationProvider(daoAuthenticationProvider(userDetailsService, passwordEncoder))
        .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(additionalHeadersFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(deviceIdFilter, UsernamePasswordAuthenticationFilter.class)

        .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint));
    return http.build();
  }

  private CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(MAX_AGE_CORS);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public UserDetailsService userDetailsService() {
    return username -> userDBService.getUser(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(UserDetailsService userDetailsService) {
    return new JwtAuthenticationFilter(jwtService, userDetailsService);
  }

  @Bean
  public SecurityHeadersFilter createSecurityHeadersFilter() {
    return new SecurityHeadersFilter();
  }

  @Bean
  public RateLimitingFilter rateLimitingFilter(
      InMemoryRateLimiter rateLimiter,
      ViolationRegistry violationRegistry,
      BlockRegistry blockRegistry,
      ApplicationProperties properties
  ) {
    return new RateLimitingFilter(rateLimiter, violationRegistry, blockRegistry, properties);
  }

  @Bean
  public DeviceIdFilter deviceIdFilter(UserDBService userDeviceDBService, RestAuthenticationEntryPoint restAuthenticationEntryPoint) {
    return new DeviceIdFilter(userDeviceDBService, cookieService, restAuthenticationEntryPoint);
  }

  @Bean
  public AdditionalHeadersFilter createAdditionalHeadersFilter() {
    return new AdditionalHeadersFilter();
  }

  private AuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
    return configuration.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
