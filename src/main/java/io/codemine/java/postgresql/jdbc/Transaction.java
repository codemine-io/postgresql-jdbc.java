package io.codemine.java.postgresql.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A unit of work run atomically against a single JDBC {@link Connection}.
 *
 * @param <R> the result type produced by {@link #run}
 */
@FunctionalInterface
public interface Transaction<R> {

  /**
   * The body of the transaction. May call {@link Statement#execute(Connection)} any number of times
   * against {@code connection}.
   *
   * @param connection the JDBC connection to run against
   * @return the result of the transaction
   * @throws SQLException if a database access error occurs
   */
  R run(Connection connection) throws SQLException;

  /**
   * Runs this transaction using {@link TransactionSettings#DEFAULT}.
   *
   * @param connection the JDBC connection to use
   * @return the result of {@link #run}
   * @throws SQLException if a database access error occurs while executing the transaction
   */
  default R execute(Connection connection) throws SQLException {
    return execute(connection, TransactionSettings.DEFAULT);
  }

  /**
   * Runs this transaction atomically: disables autocommit, applies {@code settings}, runs {@link
   * #run}, commits on success, and rolls back on any exception. The connection's original
   * autocommit, isolation level and read-only state are restored before returning or throwing.
   *
   * @param connection the JDBC connection to use
   * @param settings the settings to apply for this execution
   * @return the result of {@link #run}
   * @throws SQLException if a database access error occurs while executing the transaction
   */
  default R execute(Connection connection, TransactionSettings settings) throws SQLException {
    Objects.requireNonNull(connection, "connection");
    Objects.requireNonNull(settings, "settings");

    boolean originalAutoCommit = connection.getAutoCommit();
    int originalIsolation = connection.getTransactionIsolation();
    boolean originalReadOnly = connection.isReadOnly();

    connection.setAutoCommit(false);
    for (IsolationLevel level : settings.isolationLevel().stream().toList()) {
      connection.setTransactionIsolation(level.jdbcLevel());
    }
    connection.setReadOnly(settings.readOnly());
    try {
      for (int attempt = 1; ; attempt++) {
        try {
          R result = run(connection);
          connection.commit();
          return result;
        } catch (Exception e) {
          try {
            connection.rollback();
          } catch (SQLException suppressed) {
            e.addSuppressed(suppressed);
          }
          if (attempt >= settings.maxAttempts()) {
            throw e;
          }
          boolean retryable = false;
          if (e instanceof SQLException sqlException) {
            String state = sqlException.getSQLState();
            retryable =
                state != null
                    && (state.equals("40001") || state.equals("40P01") || state.equals("23505"));
          }
          if (!retryable) {
            throw e;
          }
        }
      }
    } finally {
      connection.setAutoCommit(originalAutoCommit);
      connection.setTransactionIsolation(originalIsolation);
      connection.setReadOnly(originalReadOnly);
    }
  }

  /**
   * Adapts a {@link Statement} into a {@code Transaction} for use in composition.
   *
   * @param statement the statement to adapt
   * @param <R> the statement's result type
   * @return a transaction that runs {@code statement} against the given connection
   */
  static <R> Transaction<R> of(Statement<R> statement) {
    Objects.requireNonNull(statement, "statement");
    return statement::execute;
  }

  /**
   * Sequences this and {@code next} into one transaction sharing the same connection: {@code next}
   * only runs if this transaction's {@link #run} completes normally, and both run within the same
   * commit/rollback boundary.
   *
   * @param next the transaction to run after this one
   * @param <R2> the result type of {@code next}
   * @return a transaction that runs this transaction, then {@code next}
   */
  default <R2> Transaction<R2> andThen(Transaction<? extends R2> next) {
    Objects.requireNonNull(next, "next");
    return connection -> {
      run(connection);
      return next.run(connection);
    };
  }

  /**
   * Transforms this transaction's result after it runs.
   *
   * @param mapper the function to apply to this transaction's result
   * @param <R2> the transformed result type
   * @return a transaction producing the mapped result
   */
  default <R2> Transaction<R2> map(Function<? super R, ? extends R2> mapper) {
    Objects.requireNonNull(mapper, "mapper");
    return connection -> mapper.apply(run(connection));
  }

  /**
   * Wraps this transaction's body in a savepoint: on {@link SQLException}, rolls back to the
   * savepoint (not the enclosing transaction) and invokes {@code onFailure} to produce a fallback
   * result instead of propagating the failure.
   *
   * @param onFailure invoked with the connection and the failure to produce a fallback result
   * @return a transaction that recovers from a {@link SQLException} via a savepoint
   */
  default Transaction<R> withSavepoint(
      BiFunction<Connection, SQLException, ? extends R> onFailure) {
    Objects.requireNonNull(onFailure, "onFailure");
    return connection -> {
      Savepoint savepoint = connection.setSavepoint();
      try {
        R result = run(connection);
        connection.releaseSavepoint(savepoint);
        return result;
      } catch (SQLException e) {
        connection.rollback(savepoint);
        return onFailure.apply(connection, e);
      }
    };
  }
}
