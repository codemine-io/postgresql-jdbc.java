package io.codemine.java.postgresql.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RetryPolicy}. */
public class RetryPolicyTest {

  @Test
  void noneNeverRetries() {
    assertEquals(1, RetryPolicy.NONE.maxAttempts());
    assertFalse(RetryPolicy.NONE.retryable().test(new SQLException("x", "40001")));
  }

  @Test
  void onSqlStatesMatchesOnlyListedStates() {
    RetryPolicy policy = RetryPolicy.onSqlStates(5, Set.of("40001", "40P01"));

    assertEquals(5, policy.maxAttempts());
    assertTrue(policy.retryable().test(new SQLException("x", "40001")));
    assertTrue(policy.retryable().test(new SQLException("x", "40P01")));
    assertFalse(policy.retryable().test(new SQLException("x", "23505")));
  }

  @Test
  void serializationFailuresMatchesSerializationAndDeadlockStates() {
    RetryPolicy policy = RetryPolicy.serializationFailures(3);

    assertEquals(3, policy.maxAttempts());
    assertTrue(policy.retryable().test(new SQLException("x", "40001")));
    assertTrue(policy.retryable().test(new SQLException("x", "40P01")));
    assertFalse(policy.retryable().test(new SQLException("x", "23505")));
  }

  @Test
  void onSqlStatesRejectsNullSet() {
    var thrown = assertThrows(NullPointerException.class, () -> RetryPolicy.onSqlStates(1, null));
    assertEquals("sqlStates", thrown.getMessage());
  }
}
