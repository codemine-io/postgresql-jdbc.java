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
    assertEquals(1, TransactionSettings.DEFAULT.maxAttempts());
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
  void withMaxAttemptsReturnsModifiedCopyWithoutMutatingOriginal() {
    TransactionSettings modified = TransactionSettings.DEFAULT.withMaxAttempts(5);

    assertEquals(5, modified.maxAttempts());
    assertEquals(1, TransactionSettings.DEFAULT.maxAttempts());
  }

  @Test
  void withIsolationLevelRejectsNull() {
    var thrown =
        assertThrows(
            NullPointerException.class, () -> TransactionSettings.DEFAULT.withIsolationLevel(null));
    assertEquals("level", thrown.getMessage());
  }

  @Test
  void withMaxAttemptsRejectsLessThanOne() {
    var thrown =
        assertThrows(
            IllegalArgumentException.class, () -> TransactionSettings.DEFAULT.withMaxAttempts(0));
    assertEquals("maxAttempts must be at least 1", thrown.getMessage());
  }
}
