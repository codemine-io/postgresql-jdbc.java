package io.codemine.java.postgresql.jdbc;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A bounded retry policy for {@link Transaction} execution, driven by the failing statement's
 * SQLSTATE.
 *
 * @param maxAttempts the maximum number of attempts, including the first; at least 1
 * @param retryable whether a given {@link SQLException} should trigger a retry
 */
public record RetryPolicy(int maxAttempts, Predicate<SQLException> retryable) {

  /**
   * Validates the record's components.
   *
   * @throws IllegalArgumentException if {@code maxAttempts} is less than 1
   */
  public RetryPolicy {
    Objects.requireNonNull(retryable, "retryable");
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be at least 1");
    }
  }

  /** A policy that never retries. */
  public static final RetryPolicy NONE = new RetryPolicy(1, e -> false);

  /**
   * A policy that retries when the failing exception's SQLSTATE is in {@code sqlStates}.
   *
   * @param maxAttempts the maximum number of attempts, including the first; at least 1
   * @param sqlStates the SQLSTATEs that should trigger a retry
   * @return a new {@code RetryPolicy}
   */
  public static RetryPolicy onSqlStates(int maxAttempts, Set<String> sqlStates) {
    Objects.requireNonNull(sqlStates, "sqlStates");
    Set<String> sqlStatesCopy = Set.copyOf(sqlStates);
    return new RetryPolicy(maxAttempts, e -> sqlStatesCopy.contains(e.getSQLState()));
  }

  /**
   * A policy that retries on PostgreSQL's {@code serialization_failure} (40001) and {@code
   * deadlock_detected} (40P01) SQLSTATEs — failures caused by concurrent activity rather than the
   * transaction's own logic, which may succeed if simply retried.
   *
   * @param maxAttempts the maximum number of attempts, including the first; at least 1
   * @return a new {@code RetryPolicy}
   */
  public static RetryPolicy serializationFailures(int maxAttempts) {
    return onSqlStates(maxAttempts, Set.of("40001", "40P01"));
  }
}
