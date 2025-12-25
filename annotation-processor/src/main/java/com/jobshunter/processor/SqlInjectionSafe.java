package com.jobshunter.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link String} field or parameter whose literal value should be checked for common
 * SQL-injection patterns at compile time.
 *
 * <p>This annotation is processed by {@link SqlInjectionValidatorProcessor}. At compile time the
 * processor inspects constant string values (including defaults from other annotations) and fails
 * the build if the value matches a known SQL-injection signature.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SqlInjectionSafe {
}


