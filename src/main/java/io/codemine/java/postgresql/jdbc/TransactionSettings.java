package io.codemine.java.postgresql.jdbc;

import java.util.Objects;
import java.util.Optional;

/**
 * Configuration for {@link Transaction#execute(java.sql.Connection, TransactionSettings)}.
 *
 * @param isolationLevel the isolation level to apply, or empty to leave the connection's current
 *     isolation level untouched
 * @param readOnly whether to mark the transaction read-only
 * @param retryPolicy the retry policy to apply on a retryable failure
 */
public record TransactionSettings(
    Optional<IsolationLevel> isolationLevel, boolean readOnly, RetryPolicy retryPolicy) {

  /** Validates the record's components. */
  public TransactionSettings {
    Objects.requireNonNull(isolationLevel, "isolationLevel");
    Objects.requireNonNull(retryPolicy, "retryPolicy");
  }

  /** Default settings: no isolation-level override, not read-only, no retries. */
  public static final TransactionSettings DEFAULT =
      new TransactionSettings(Optional.empty(), false, RetryPolicy.NONE);

  /**
   * Returns a copy of these settings with the given isolation level.
   *
   * @param level the isolation level to apply
   * @return a new {@code TransactionSettings}
   */
  public TransactionSettings withIsolationLevel(IsolationLevel level) {
    Objects.requireNonNull(level, "level");
    return new TransactionSettings(Optional.of(level), readOnly, retryPolicy);
  }

  /**
   * Returns a copy of these settings with the given read-only flag.
   *
   * @param readOnly whether the transaction should be read-only
   * @return a new {@code TransactionSettings}
   */
  public TransactionSettings withReadOnly(boolean readOnly) {
    return new TransactionSettings(isolationLevel, readOnly, retryPolicy);
  }

  /**
   * Returns a copy of these settings with the given retry policy.
   *
   * @param retryPolicy the retry policy to apply
   * @return a new {@code TransactionSettings}
   */
  public TransactionSettings withRetryPolicy(RetryPolicy retryPolicy) {
    Objects.requireNonNull(retryPolicy, "retryPolicy");
    return new TransactionSettings(isolationLevel, readOnly, retryPolicy);
  }
}
