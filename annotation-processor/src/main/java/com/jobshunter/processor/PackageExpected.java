package com.jobshunter.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the package where the annotated class is expected to reside.
 * Used by {@code PackageUsageProcessor} (compile time) and {@link PackageExpectedValidator} (runtime).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface PackageExpected {
  /**
   * Fully qualified package name the annotated class must belong to.
   */
  String value();
}
