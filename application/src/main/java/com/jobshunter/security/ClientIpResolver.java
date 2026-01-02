package com.jobshunter.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

public final class ClientIpResolver {

  private static final Pattern IPV4 = Pattern.compile("^(?:\\d{1,3}\\.){3}\\d{1,3}$");

  // Acceptă forme uzuale de IPv6 (nu e validare completă, dar suficientă pentru log/rate-limit key).
  private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:]+(%[0-9a-zA-Z._-]+)?$");

  private ClientIpResolver() {
  }

  public static String resolveClientIp(HttpServletRequest request) {
    String ip = fromForwardedHeader(request);
    if (isValidIp(ip)) {
      return ip;
    }

    ip = firstFromXForwardedFor(request.getHeader("X-Forwarded-For"));
    if (isValidIp(ip)) {
      return ip;
    }

    ip = request.getHeader("X-Real-IP");
    if (isValidIp(ip)) {
      return ip;
    }

    return request.getRemoteAddr();
  }

  private static String fromForwardedHeader(HttpServletRequest request) {
    // Exemplu: Forwarded: for=203.0.113.60;proto=https;by=203.0.113.43
    // sau:     Forwarded: for="[2001:db8:cafe::17]:4711"
    String forwarded = request.getHeader("Forwarded");
    if (forwarded == null || forwarded.isBlank()) {
      return null;
    }

    // Căutăm primul "for="
    // Nu implementăm parser complet RFC, doar extragem robust cazul comun.
    String lower = forwarded.toLowerCase();
    int idx = lower.indexOf("for=");
    if (idx < 0) {
      return null;
    }

    String value = forwarded.substring(idx + 4);
    int end = value.indexOf(';');
    if (end >= 0) {
      value = value.substring(0, end);
    }

    value = value.trim();

    // poate fi între ghilimele
    if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
      value = value.substring(1, value.length() - 1);
    }

    // poate fi [IPv6]:port
    if (value.startsWith("[") && value.contains("]")) {
      int close = value.indexOf(']');
      return value.substring(1, close);
    }

    // poate fi ip:port (IPv4)
    int colon = value.indexOf(':');
    if (colon > 0 && value.indexOf(':', colon + 1) < 0) { // un singur ':'
      return value.substring(0, colon);
    }

    return value;
  }

  private static String firstFromXForwardedFor(String xff) {
    if (xff == null || xff.isBlank()) {
      return null;
    }
    // XFF: client, proxy1, proxy2
    String first = xff.split(",")[0].trim();

    // poate conține port
    int colon = first.indexOf(':');
    if (colon > 0 && first.indexOf(':', colon + 1) < 0) { // IPv4:port
      first = first.substring(0, colon);
    }

    // poate fi [IPv6]
    if (first.startsWith("[") && first.endsWith("]")) {
      first = first.substring(1, first.length() - 1);
    }

    return first;
  }

  private static boolean isValidIp(String ip) {
    if (ip == null || ip.isBlank()) {
      return false;
    }
    ip = ip.trim();

    // Exclude values like "unknown"
    if ("unknown".equalsIgnoreCase(ip)) {
      return false;
    }

    if (IPV4.matcher(ip).matches()) {
      // Validare minimă octeți 0-255
      String[] parts = ip.split("\\.");
      for (String part : parts) {
        int value = Integer.parseInt(part);
        if (value < 0 || value > 255) {
          return false;
        }
      }
      return true;
    }

    return IPV6.matcher(ip).matches();
  }
}
