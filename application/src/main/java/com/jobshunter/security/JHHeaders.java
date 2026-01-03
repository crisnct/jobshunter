package com.jobshunter.security;

import org.springframework.http.HttpHeaders;

public class JHHeaders extends HttpHeaders {

  public static final String IP_HEADER = "JHIPHEADER";

  public static final String X_FORWARDED_FOR = "X-Forwarded-For";

  public static final String X_REAL_IP = "X-Real-IP";

  public static final String FORWARDED = "Forwarded";

  public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

  public static final String X_XSRF_TOKEN = "X-XSRF-TOKEN";

  public static final String X_FRAME_OPTIONS = "X-Frame-Options";

  public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

  public static final String X_XSS_PROTECTION = "X-XSS-Protection";

  public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";

  public static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";

  public static final String REFERRER_POLICY = "Referrer-Policy";

  public static final String X_PERMITTED_CROSS_DOMAIN_POLICIES = "X-Permitted-Cross-Domain-Policies";

  public static final String PERMISSIONS_POLICY = "Permissions-Policy";

  public static final String CROSS_ORIGIN_EMBEDDER_POLICY = "Cross-Origin-Embedder-Policy";

  public static final String CROSS_ORIGIN_OPENER_POLICY = "Cross-Origin-Opener-Policy";

  public static final String CROSS_ORIGIN_RESOURCE_POLICY = "Cross-Origin-Resource-Policy";

}
