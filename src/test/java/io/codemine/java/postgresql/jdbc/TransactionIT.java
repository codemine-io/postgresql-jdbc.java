package io.codemine.java.postgresql.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

/** Integration tests for {@link Transaction}. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TransactionIT {

  private static final String TABLE_NAME = "transaction_it";

  static final PostgreSQLContainer<?> container;
  static final HikariDataSource jdbcPool;

  static {
    container =
        new PostgreSQLContainer<>("postgres:18").withCommand("postgres -c max_connections=300");
    container.start();

    var hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(container.getJdbcUrl());
    hikariConfig.setUsername(container.getUsername());
    hikariConfig.setPassword(container.getPassword());
    hikariConfig.addDataSourceProperty("prepareThreshold", "0");
    hikariConfig.setMaximumPoolSize(10);
    jdbcPool = new HikariDataSource(hikariConfig);

    Runtime.getRuntime().addShutdownHook(new Thread(jdbcPool::close));
  }

  @SuppressWarnings("unused")
  @BeforeEach
  void resetTable() throws SQLException {
    try (var conn = jdbcPool.getConnection();
        var ps = conn.createStatement()) {
      ps.executeUpdate("DROP TABLE IF EXISTS " + TABLE_NAME);
      ps.executeUpdate(
          "CREATE TABLE " + TABLE_NAME + " (id INTEGER PRIMARY KEY, value TEXT NOT NULL)");
    }
  }

  @Test
  void executeCommitsOnSuccessfulRun() throws Exception {
    Transaction<Void> transaction =
        connection -> {
          new InsertRow(1, "one").execute(connection);
          new InsertRow(2, "two").execute(connection);
          return null;
        };

    try (var conn = jdbcPool.getConnection()) {
      transaction.execute(conn);
    }

    assertRowCount(2);
  }

  @Test
  void executeRollsBackOnSqlExceptionFromRun() throws Exception {
    Transaction<Void> transaction =
        connection -> {
          new InsertRow(1, "one").execute(connection);
          throw new SQLException("simulated failure");
        };

    try (var conn = jdbcPool.getConnection()) {
      assertThrows(SQLException.class, () -> transaction.execute(conn));
    }

    assertRowCount(0);
  }

  @Test
  void executeRollsBackOnRuntimeExceptionFromRun() throws Exception {
    Transaction<Void> transaction =
        connection -> {
          new InsertRow(1, "one").execute(connection);
          throw new IllegalStateException("simulated failure");
        };

    try (var conn = jdbcPool.getConnection()) {
      assertThrows(IllegalStateException.class, () -> transaction.execute(conn));
    }

    assertRowCount(0);
  }

  @Test
  void executeRestoresOriginalAutoCommitAfterSuccess() throws Exception {
    Transaction<Void> transaction =
        connection -> {
          new InsertRow(1, "one").execute(connection);
          return null;
        };

    try (var conn = jdbcPool.getConnection()) {
      conn.setAutoCommit(true);
      transaction.execute(conn);
      assertTrue(conn.getAutoCommit());
    }
  }

  @Test
  void executeRestoresOriginalAutoCommitAfterFailure() throws Exception {
    Transaction<Void> transaction =
        connection -> {
          throw new SQLException("simulated failure");
        };

    try (var conn = jdbcPool.getConnection()) {
      conn.setAutoCommit(true);
      assertThrows(SQLException.class, () -> transaction.execute(conn));
      assertTrue(conn.getAutoCommit());
    }
  }

  @Test
  void executeRejectsNullConnection() {
    Transaction<Void> transaction = connection -> null;

    var thrown = assertThrows(NullPointerException.class, () -> transaction.execute(null));
    assertEquals("connection", thrown.getMessage());
  }

  @Test
  void ofAdaptsStatementIntoTransaction() throws Exception {
    Transaction<Void> transaction = Transaction.of(new InsertRow(1, "one"));

    try (var conn = jdbcPool.getConnection()) {
      transaction.execute(conn);
    }

    assertRowCount(1);
  }

  @Test
  void executeAppliesIsolationLevelFromSettings() throws Exception {
    int[] observedIsolation = new int[1];
    Transaction<Void> transaction =
        connection -> {
          observedIsolation[0] = connection.getTransactionIsolation();
          return null;
        };

    try (var conn = jdbcPool.getConnection()) {
      transaction.execute(
          conn, TransactionSettings.DEFAULT.withIsolationLevel(IsolationLevel.SERIALIZABLE));
    }

    assertEquals(Connection.TRANSACTION_SERIALIZABLE, observedIsolation[0]);
  }

  @Test
  void executeRestoresOriginalIsolationLevelAfterExecute() throws Exception {
    Transaction<Void> transaction = connection -> null;

    try (var conn = jdbcPool.getConnection()) {
      conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
      transaction.execute(
          conn, TransactionSettings.DEFAULT.withIsolationLevel(IsolationLevel.SERIALIZABLE));
      assertEquals(Connection.TRANSACTION_READ_COMMITTED, conn.getTransactionIsolation());
    }
  }

  @Test
  void executeAppliesReadOnlyFromSettings() throws Exception {
    boolean[] observedReadOnly = new boolean[1];
    Transaction<Void> transaction =
        connection -> {
          observedReadOnly[0] = connection.isReadOnly();
          return null;
        };

    try (var conn = jdbcPool.getConnection()) {
      transaction.execute(conn, TransactionSettings.DEFAULT.withReadOnly(true));
    }

    assertTrue(observedReadOnly[0]);
  }

  @Test
  void executeRestoresOriginalReadOnlyAfterExecute() throws Exception {
    Transaction<Void> transaction = connection -> null;

    try (var conn = jdbcPool.getConnection()) {
      conn.setReadOnly(false);
      transaction.execute(conn, TransactionSettings.DEFAULT.withReadOnly(true));
      assertTrue(!conn.isReadOnly());
    }
  }

  @Test
  void executeRejectsNullSettings() throws Exception {
    Transaction<Void> transaction = connection -> null;

    try (var conn = jdbcPool.getConnection()) {
      var thrown = assertThrows(NullPointerException.class, () -> transaction.execute(conn, null));
      assertEquals("settings", thrown.getMessage());
    }
  }

  @Test
  void executeRetriesUntilSuccessOnRetryableSqlState() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    Transaction<Integer> transaction =
        connection -> {
          int attempt = attempts.incrementAndGet();
          new InsertRow(attempt, "attempt-" + attempt).execute(connection);
          if (attempt < 3) {
            throw new SQLException("simulated conflict", "40001");
          }
          return attempt;
        };

    int result;
    try (var conn = jdbcPool.getConnection()) {
      result = transaction.execute(conn, TransactionSettings.DEFAULT.withMaxAttempts(5));
    }

    assertEquals(3, result);
    assertEquals(3, attempts.get());
    // Failed attempts' inserts were rolled back; only the successful attempt's row survives.
    assertRowCount(1);
  }

  @Test
  void executeGivesUpAfterMaxAttempts() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    Transaction<Void> transaction =
        connection -> {
          attempts.incrementAndGet();
          throw new SQLException("simulated conflict", "40001");
        };

    try (var conn = jdbcPool.getConnection()) {
      assertThrows(
          SQLException.class,
          () -> transaction.execute(conn, TransactionSettings.DEFAULT.withMaxAttempts(2)));
    }

    assertEquals(2, attempts.get());
  }

  @Test
  void executeRetriesOnUniqueViolationSqlState() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    Transaction<Void> transaction =
        connection -> {
          if (attempts.incrementAndGet() < 3) {
            throw new SQLException("unique violation", "23505");
          }
          return null;
        };

    try (var conn = jdbcPool.getConnection()) {
      transaction.execute(conn, TransactionSettings.DEFAULT.withMaxAttempts(5));
    }

    assertEquals(3, attempts.get());
  }

  @Test
  void executeDoesNotRetryNonRetryableSqlState() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    Transaction<Void> transaction =
        connection -> {
          attempts.incrementAndGet();
          throw new SQLException("foreign key violation", "23503");
        };

    try (var conn = jdbcPool.getConnection()) {
      assertThrows(
          SQLException.class,
          () -> transaction.execute(conn, TransactionSettings.DEFAULT.withMaxAttempts(5)));
    }

    assertEquals(1, attempts.get());
  }

  @Test
  void andThenCommitsBothStatementsAsOneTransaction() throws Exception {
    Transaction<Void> transaction =
        Transaction.of(new InsertRow(1, "one")).andThen(Transaction.of(new InsertRow(2, "two")));

    try (var conn = jdbcPool.getConnection()) {
      transaction.execute(conn);
    }

    assertRowCount(2);
  }

  @Test
  void andThenRollsBackFirstStatementWhenSecondFails() throws Exception {
    Transaction<Void> failing =
        connection -> {
          throw new SQLException("simulated failure");
        };
    Transaction<Void> transaction = Transaction.of(new InsertRow(1, "one")).andThen(failing);

    try (var conn = jdbcPool.getConnection()) {
      assertThrows(SQLException.class, () -> transaction.execute(conn));
    }

    assertRowCount(0);
  }

  @Test
  void mapTransformsResult() throws Exception {
    Transaction<Integer> transaction =
        connection -> {
          new InsertRow(1, "one").execute(connection);
          return 1;
        };
    Transaction<String> mapped = transaction.map(count -> "count=" + count);

    String result;
    try (var conn = jdbcPool.getConnection()) {
      result = mapped.execute(conn);
    }

    assertEquals("count=1", result);
  }

  @Test
  void withSavepointRecoversWithoutRollingBackWholeTransaction() throws Exception {
    Transaction<Void> failingInsert =
        connection -> {
          new InsertRow(2, "two").execute(connection);
          throw new SQLException("simulated failure");
        };
    Transaction<Void> outer =
        connection -> {
          new InsertRow(1, "one").execute(connection);
          failingInsert.withSavepoint((conn, e) -> null).run(connection);
          return null;
        };

    try (var conn = jdbcPool.getConnection()) {
      outer.execute(conn);
    }

    // Row 1 survives (whole transaction commits); row 2 was rolled back to the savepoint.
    assertRowCount(1);
  }

  private static void assertRowCount(int expected) throws SQLException {
    try (var conn = jdbcPool.getConnection();
        var ps = conn.prepareStatement("SELECT COUNT(*) FROM " + TABLE_NAME);
        ResultSet rs = ps.executeQuery()) {
      assertTrue(rs.next());
      assertEquals(expected, rs.getInt(1));
    }
  }

  private record InsertRow(int id, String value) implements Statement<Void> {

    @Override
    public String sql() {
      return "INSERT INTO " + TABLE_NAME + " (id, value) VALUES (?, ?)";
    }

    @Override
    public void bindParams(PreparedStatement ps) throws SQLException {
      ps.setInt(1, id);
      ps.setString(2, value);
    }

    @Override
    public boolean returnsRows() {
      return false;
    }

    @Override
    public Void decodeResultSet(ResultSet rs) {
      throw new UnsupportedOperationException("Not used");
    }

    @Override
    public Void decodeAffectedRows(long affectedRows) {
      return null;
    }
  }
}
