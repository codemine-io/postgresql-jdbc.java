package io.codemine.java.postgresql.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link TransactionSettings}. */
public class TransactionSettingsTest {

  @Test
  void defaultHasNoIsolationOverrideAndIsNotReadOnlyAndNeverRetries() {
    assertTrue(TransactionSettings.DEFAULT.isolationLevel().isEmpty());
    assertFalse(TransactionSettings.DEFAULT.readOnly());
    assertEquals(RetryPolicy.NONE, TransactionSettings.DEFAULT.retryPolicy());
  }

  @Test
  void withIsolationLevelReturnsModifiedCopyWithoutMutatingOriginal() {
    TransactionSettings modified =
        TransactionSettings.DEFAULT.withIsolationLevel(IsolationLevel.SERIALIZABLE);

    assertEquals(IsolationLevel.SERIALIZABLE, modified.isolationLevel().orElseThrow());
    assertTrue(TransactionSettings.DEFAULT.isolationLevel().isEmpty());
  }

  @Test
  void withReadOnlyReturnsModifiedCopyWithoutMutatingOriginal() {
    TransactionSettings modified = TransactionSettings.DEFAULT.withReadOnly(true);

    assertTrue(modified.readOnly());
    assertFalse(TransactionSettings.DEFAULT.readOnly());
  }

  @Test
  void withRetryPolicyReturnsModifiedCopyWithoutMutatingOriginal() {
    RetryPolicy policy = RetryPolicy.serializationFailures(5);
    TransactionSettings modified = TransactionSettings.DEFAULT.withRetryPolicy(policy);

    assertEquals(policy, modified.retryPolicy());
    assertEquals(RetryPolicy.NONE, TransactionSettings.DEFAULT.retryPolicy());
  }

  @Test
  void withIsolationLevelRejectsNull() {
    var thrown =
        assertThrows(
            NullPointerException.class, () -> TransactionSettings.DEFAULT.withIsolationLevel(null));
    assertEquals("level", thrown.getMessage());
  }

  @Test
  void withRetryPolicyRejectsNull() {
    var thrown =
        assertThrows(
            NullPointerException.class, () -> TransactionSettings.DEFAULT.withRetryPolicy(null));
    assertEquals("retryPolicy", thrown.getMessage());
  }
}
