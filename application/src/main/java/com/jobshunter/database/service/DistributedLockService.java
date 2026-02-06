package com.jobshunter.database.service;

import jakarta.persistence.EntityManager;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Distributed locking service using MySQL GET_LOCK/RELEASE_LOCK for cross-instance coordination.
 * Locks are held at the database level, ensuring mutual exclusion across all application instances.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

  private final EntityManager entityManager;

  private static final int LOCK_TIMEOUT_SECONDS = 30;

  /**
   * Acquires a named lock. Returns true if acquired, false if timeout.
   * Lock is held until releaseLock() is called or connection closes.
   */
  public boolean acquireLock(String lockName) {
    try {
      Object result = entityManager
          .createNativeQuery("SELECT GET_LOCK(:lockName, :timeout)")
          .setParameter("lockName", lockName)
          .setParameter("timeout", LOCK_TIMEOUT_SECONDS)
          .getSingleResult();
      boolean acquired = result != null && ((Number) result).intValue() == 1;
      if (acquired) {
        log.debug("Acquired distributed lock: {}", lockName);
      } else {
        log.warn("Failed to acquire distributed lock: {} (timeout)", lockName);
      }
      return acquired;
    } catch (Exception e) {
      log.error("Error acquiring distributed lock {}: {}", lockName, e.getMessage());
      return false;
    }
  }

  /**
   * Releases a named lock. Returns true if released, false if not held.
   */
  public boolean releaseLock(String lockName) {
    try {
      Object result = entityManager
          .createNativeQuery("SELECT RELEASE_LOCK(:lockName)")
          .setParameter("lockName", lockName)
          .getSingleResult();
      boolean released = result != null && ((Number) result).intValue() == 1;
      if (released) {
        log.debug("Released distributed lock: {}", lockName);
      }
      return released;
    } catch (Exception e) {
      log.error("Error releasing distributed lock {}: {}", lockName, e.getMessage());
      return false;
    }
  }

  /**
   * Executes action while holding the lock. Automatically releases on completion.
   *
   * @param lockName the name of the lock to acquire
   * @param action   the action to execute while holding the lock
   * @param <T>      the return type of the action
   * @return the result of the action
   * @throws IllegalStateException if the lock cannot be acquired
   */
  public <T> T executeWithLock(String lockName, Supplier<T> action) {
    if (!acquireLock(lockName)) {
      throw new IllegalStateException("Could not acquire lock: " + lockName);
    }
    try {
      return action.get();
    } finally {
      releaseLock(lockName);
    }
  }

  /**
   * Executes action while holding the lock. Automatically releases on completion.
   *
   * @param lockName the name of the lock to acquire
   * @param action   the action to execute while holding the lock
   * @throws IllegalStateException if the lock cannot be acquired
   */
  public void executeWithLock(String lockName, Runnable action) {
    executeWithLock(lockName, () -> {
      action.run();
      return null;
    });
  }
}
