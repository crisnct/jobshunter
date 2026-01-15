package com.jobshunter.dto;

//@formatter:off
/**
 * Explicit copy contract. Replaces instanceof-based copying.
 */
//@formatter:on
public interface Copyable<T> {

  T copy();
}
