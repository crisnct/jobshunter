package com.jobshunter.security;

import org.springframework.http.HttpHeaders;

public class JHHeaders extends HttpHeaders {

  public static final String IP_HEADER = "JHIPHEADER";

  public static final String X_FORWARDED_FOR = "X-Forwarded-For";

  public static final String X_REAL_IP = "X-Real-IP";

  public static final String FORWARDED = "Forwarded";

  public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

}
