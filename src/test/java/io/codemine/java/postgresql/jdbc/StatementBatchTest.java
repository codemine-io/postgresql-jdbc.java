package io.codemine.java.postgresql.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link StatementBatch}'s query-timeout handling. */
class StatementBatchTest {

  @Test
  void executeAppliesPositiveQueryTimeoutToThePreparedStatement() throws Exception {
    RecordingPreparedStatement recorder = new RecordingPreparedStatement();
    Connection connection = connectionReturning(recorder.proxy());
    StatementBatch<Void> batch =
        new StatementBatch<>(List.of(new FakeUpdateStatement(), new FakeUpdateStatement()));

    batch.execute(connection, 5);

    assertEquals(List.of(5), recorder.queryTimeoutsSet);
  }

  @Test
  void executeWithZeroTimeoutLeavesTheDriverDefaultInPlace() throws Exception {
    RecordingPreparedStatement recorder = new RecordingPreparedStatement();
    Connection connection = connectionReturning(recorder.proxy());
    StatementBatch<Void> batch = new StatementBatch<>(List.of(new FakeUpdateStatement()));

    batch.execute(connection, 0);

    assertTrue(recorder.queryTimeoutsSet.isEmpty());
  }

  @Test
  void executeWithNegativeTimeoutLeavesTheDriverDefaultInPlace() throws Exception {
    RecordingPreparedStatement recorder = new RecordingPreparedStatement();
    Connection connection = connectionReturning(recorder.proxy());
    StatementBatch<Void> batch = new StatementBatch<>(List.of(new FakeUpdateStatement()));

    batch.execute(connection, -1);

    assertTrue(recorder.queryTimeoutsSet.isEmpty());
  }

  @Test
  void executeWithoutATimeoutArgumentLeavesTheDriverDefaultInPlace() throws Exception {
    RecordingPreparedStatement recorder = new RecordingPreparedStatement();
    Connection connection = connectionReturning(recorder.proxy());
    StatementBatch<Void> batch = new StatementBatch<>(List.of(new FakeUpdateStatement()));

    batch.execute(connection);

    assertTrue(recorder.queryTimeoutsSet.isEmpty());
  }

  private static Connection connectionReturning(PreparedStatement preparedStatement) {
    InvocationHandler handler =
        (proxy, method, args) -> {
          if (method.getName().equals("prepareStatement")) {
            return preparedStatement;
          }
          throw new UnsupportedOperationException(method.getName());
        };
    return (Connection)
        Proxy.newProxyInstance(
            Connection.class.getClassLoader(), new Class<?>[] {Connection.class}, handler);
  }

  /** A {@link PreparedStatement} fake that records {@code setQueryTimeout} calls. */
  private static final class RecordingPreparedStatement implements InvocationHandler {
    final List<Integer> queryTimeoutsSet = new ArrayList<>();
    private int batchedCount;

    PreparedStatement proxy() {
      return (PreparedStatement)
          Proxy.newProxyInstance(
              PreparedStatement.class.getClassLoader(),
              new Class<?>[] {PreparedStatement.class},
              this);
    }

    @Override
    public Object invoke(Object proxyInstance, Method method, Object[] args) {
      switch (method.getName()) {
        case "setQueryTimeout" -> queryTimeoutsSet.add((Integer) args[0]);
        case "addBatch" -> batchedCount++;
        case "executeBatch" -> {
          return new int[batchedCount];
        }
        case "clearParameters", "close" -> {
          return null;
        }
        default -> throw new UnsupportedOperationException(method.getName());
      }
      return null;
    }
  }

  private static class FakeUpdateStatement implements Statement<Void> {
    @Override
    public String sql() {
      return "UPDATE table SET value = ?";
    }

    @Override
    public void bindParams(PreparedStatement ps) {
      Objects.requireNonNull(ps, "ps");
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
