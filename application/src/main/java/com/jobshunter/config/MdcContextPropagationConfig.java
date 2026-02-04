package com.jobshunter.config;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for automatic MDC context propagation across thread boundaries.
 * <p>
 * Uses Micrometer Context Propagation to automatically capture and restore
 * MDC context (including correlationId) when tasks are executed on different threads.
 */
@Configuration
public class MdcContextPropagationConfig {

  @PostConstruct
  public void registerMdcAccessor() {
    ContextRegistry.getInstance().registerThreadLocalAccessor(new MdcThreadLocalAccessor());
  }

  /**
   * ThreadLocalAccessor implementation for SLF4J MDC.
   * Captures the entire MDC context map and restores it on worker threads.
   */
  private static class MdcThreadLocalAccessor implements ThreadLocalAccessor<Map<String, String>> {

    private static final String KEY = "mdc";

    @Override
    public String key() {
      return KEY;
    }

    @Override
    public Map<String, String> getValue() {
      return MDC.getCopyOfContextMap();
    }

    @Override
    public void setValue(Map<String, String> value) {
      if (value != null && !value.isEmpty()) {
        MDC.setContextMap(value);
      } else {
        MDC.clear();
      }
    }

    @Override
    public void setValue() {
      MDC.clear();
    }
  }
}
