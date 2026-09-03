package com.jobshunter.security;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.service.OAuth2UserDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.security.filters.AdditionalHeadersFilter;
import com.jobshunter.security.filters.CorrelationIdFilter;
import com.jobshunter.security.filters.DeviceIdFilter;
import com.jobshunter.security.filters.JwtAuthenticationFilter;
import com.jobshunter.security.filters.RateLimitingFilter;
import com.jobshunter.security.filters.SecurityHeadersFilter;
import com.jobshunter.security.rateLimitBucket4J.BlockRegistry;
import com.jobshunter.security.rateLimitBucket4J.InMemoryRateLimiter;
import com.jobshunter.security.rateLimitBucket4J.ViolationRegistry;
import com.jobshunter.service.application.JwtService;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(DelegatedAuthProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

  private static final int MAX_AGE_HSTS = (int) TimeUnit.DAYS.toSeconds(365);

  private final UserDBService userDBService;
  private final CookieService cookieService;
  private final JwtService jwtService;
  private final ApplicationProperties properties;
  private final DelegatedAuthProperties delegatedAuthProperties;
  private final OAuth2UserDBService oAuth2UserDBService;
  private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
  private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

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
  @Order(1)
  public SecurityFilterChain internalApiSecurityFilterChain(
      HttpSecurity http,
      @Qualifier("delegatedJwtDecoder") JwtDecoder delegatedJwtDecoder,
      SecurityHeadersFilter securityHeadersFilter,
      AdditionalHeadersFilter additionalHeadersFilter,
      RateLimitingFilter rateLimitingFilter,
      CorrelationIdFilter correlationIdFilter,
      RestAuthenticationEntryPoint restAuthenticationEntryPoint
  ) {
    http.securityMatcher("/api/internal/**")
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> {
           auth.anyRequest().authenticated();
        })
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
            .decoder(delegatedJwtDecoder)
            .jwtAuthenticationConverter(internalJwtAuthenticationConverter())))
        .headers(h ->
            h.httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(MAX_AGE_HSTS)
                .preload(true)
                .requestMatcher(AnyRequestMatcher.INSTANCE))
        )
        .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(additionalHeadersFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint));

    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      SecurityHeadersFilter securityHeadersFilter,
      AdditionalHeadersFilter additionalHeadersFilter,
      DeviceIdFilter deviceIdFilter,
      RateLimitingFilter rateLimitingFilter,
      CorrelationIdFilter correlationIdFilter,
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
            .ignoringRequestMatchers("/api/internal/**")
            .ignoringRequestMatchers("/oauth2/**", "/login/oauth2/**")
            .ignoringRequestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**")
        )
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/profile", "/dashboard", "/index.html", "/css/**", "/js/**", "/images/**").permitAll()
            .requestMatchers("/assets/**", "/src/**", "/favicon.jpg", "/manifest.webmanifest", "/robots.txt").permitAll()
            .requestMatchers("/legal/**").permitAll()
            .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/openapi.yml").permitAll()
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
            .authorizationEndpoint(auth -> auth.baseUri("/oauth2/authorization"))
            .redirectionEndpoint(redir -> redir.baseUri("/login/oauth2/code/*"))
            .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserDBService))
            .successHandler(oAuth2AuthenticationSuccessHandler)
            .failureHandler(oAuth2AuthenticationFailureHandler)
        )
        .headers(h ->
            h.httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(MAX_AGE_HSTS)
                .preload(true)
                .requestMatcher(AnyRequestMatcher.INSTANCE))
        )
        .authenticationProvider(daoAuthenticationProvider(userDetailsService, passwordEncoder))
        .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(additionalHeadersFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(deviceIdFilter, UsernamePasswordAuthenticationFilter.class)

        .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint));
    return http.build();
  }

  private CorsConfigurationSource corsConfigurationSource() {
    var corsProps = properties.getSecurity().getCors();
    CorsConfiguration config = new CorsConfiguration();

    List<String> origins = corsProps.getAllowedOrigins();
    if (origins.isEmpty()) {
      log.warn("No CORS origins configured - CORS will be restrictive");
    }
    config.setAllowedOriginPatterns(origins);
    config.setAllowedMethods(corsProps.getAllowedMethods());
    config.setAllowedHeaders(corsProps.getAllowedHeaders());
    config.setAllowCredentials(corsProps.isAllowCredentials());
    config.setMaxAge(corsProps.getMaxAgeSec());

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
  public JwtAuthenticationFilter jwtAuthenticationFilter(
      UserDetailsService userDetailsService
  ) {
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

  @Bean
  public CorrelationIdFilter correlationIdFilter() {
    return new CorrelationIdFilter();
  }

  @Bean
  public JwtDecoder delegatedJwtDecoder() {
    NimbusJwtDecoder jwtDecoder = createDelegatedJwtDecoder();
    OAuth2TokenValidator<Jwt> issuerValidator =
        JwtValidators.createDefaultWithIssuer(delegatedAuthProperties.issuerUri());
    OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
      List<String> audiences = jwt.getAudience();
      if (audiences != null && audiences.contains(delegatedAuthProperties.audience())) {
        return OAuth2TokenValidatorResult.success();
      }
      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error("invalid_token", "Delegated token audience mismatch", null)
      );
    };
    OAuth2TokenValidator<Jwt> scopeValidator = jwt -> {
      if (!StringUtils.hasText(delegatedAuthProperties.requiredScope())) {
        return OAuth2TokenValidatorResult.success();
      }

      String scopeClaim = jwt.getClaimAsString("scope");
      if (!StringUtils.hasText(scopeClaim)) {
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error("invalid_token", "Delegated token scope claim is missing", null)
        );
      }

      boolean hasRequiredScope = Arrays.stream(scopeClaim.split("\\s+"))
          .anyMatch(scope -> scope.equals(delegatedAuthProperties.requiredScope()));
      if (hasRequiredScope) {
        return OAuth2TokenValidatorResult.success();
      }
      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error("invalid_token", "Delegated token required scope is missing", null)
      );
    };
    OAuth2TokenValidator<Jwt> tokenUseValidator = jwt -> {
      if (!StringUtils.hasText(delegatedAuthProperties.requiredTokenUse())) {
        return OAuth2TokenValidatorResult.success();
      }

      String tokenUse = jwt.getClaimAsString("token_use");
      if (delegatedAuthProperties.requiredTokenUse().equals(tokenUse)) {
        return OAuth2TokenValidatorResult.success();
      }
      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error("invalid_token", "Delegated token_use claim is invalid", null)
      );
    };
    jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        issuerValidator,
        audienceValidator,
        scopeValidator,
        tokenUseValidator
    ));
    return jwtDecoder;
  }

  private NimbusJwtDecoder createDelegatedJwtDecoder() {
    if (StringUtils.hasText(delegatedAuthProperties.jwksUri())) {
      return NimbusJwtDecoder.withJwkSetUri(delegatedAuthProperties.jwksUri()).build();
    }
    return NimbusJwtDecoder.withIssuerLocation(delegatedAuthProperties.issuerUri()).build();
  }

  private Converter<Jwt, ? extends AbstractAuthenticationToken> internalJwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName("scope");
    authoritiesConverter.setAuthorityPrefix("SCOPE_");

    return jwt -> {
      Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);
      String principal = jwt.getClaimAsString("email");
      if (!StringUtils.hasText(principal)) {
        principal = jwt.getSubject();
      }
      return new JwtAuthenticationToken(jwt, authorities == null ? List.of() : authorities, principal);
    };
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
