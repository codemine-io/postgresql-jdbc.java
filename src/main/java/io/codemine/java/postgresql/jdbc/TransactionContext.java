package io.codemine.java.postgresql.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Objects;

/**
 * The environment a {@link Transaction} runs its body and its transaction-control bookkeeping
 * against. Every member is abstract by design: implementors — including instrumentation wrappers
 * that intercept only {@link #execute(Statement)} — must explicitly provide each behavior rather
 * than inherit a {@link Connection}-backed default. Use {@link #of(Connection)} to obtain a plain
 * JDBC-backed implementation.
 */
public interface TransactionContext {

  /**
   * Executes {@code statement}.
   *
   * @param statement the statement to execute
   * @param <R> the statement's result type
   * @return the decoded statement result
   * @throws SQLException if a database access error occurs while executing the statement
   */
  <R> R execute(Statement<R> statement) throws SQLException;

  /**
   * Executes {@code batch}.
   *
   * @param batch the batch to execute
   * @param <R> the batch's result type
   * @return the decoded results, in the same order as the batch's statements
   * @throws SQLException if a database access error occurs during execution
   */
  <R> ArrayList<R> execute(StatementBatch<R> batch) throws SQLException;

  /**
   * Returns whether auto-commit is currently enabled.
   *
   * @return whether auto-commit is currently enabled
   * @throws SQLException if a database access error occurs
   */
  boolean getAutoCommit() throws SQLException;

  /**
   * Sets whether auto-commit is enabled.
   *
   * @param autoCommit whether to enable auto-commit
   * @throws SQLException if a database access error occurs
   */
  void setAutoCommit(boolean autoCommit) throws SQLException;

  /**
   * Returns the current transaction isolation level.
   *
   * @return the current transaction isolation level, as a {@link Connection} constant
   * @throws SQLException if a database access error occurs
   */
  int getTransactionIsolation() throws SQLException;

  /**
   * Sets the transaction isolation level.
   *
   * @param level the transaction isolation level to set, as a {@link Connection} constant
   * @throws SQLException if a database access error occurs
   */
  void setTransactionIsolation(int level) throws SQLException;

  /**
   * Returns whether the current transaction is read-only.
   *
   * @return whether the current transaction is read-only
   * @throws SQLException if a database access error occurs
   */
  boolean isReadOnly() throws SQLException;

  /**
   * Sets whether the transaction is read-only.
   *
   * @param readOnly whether to mark the transaction read-only
   * @throws SQLException if a database access error occurs
   */
  void setReadOnly(boolean readOnly) throws SQLException;

  /**
   * Commits the current transaction.
   *
   * @throws SQLException if a database access error occurs
   */
  void commit() throws SQLException;

  /**
   * Rolls back the current transaction.
   *
   * @throws SQLException if a database access error occurs
   */
  void rollback() throws SQLException;

  /**
   * Rolls back to {@code savepoint}.
   *
   * @param savepoint the savepoint to roll back to
   * @throws SQLException if a database access error occurs
   */
  void rollback(Savepoint savepoint) throws SQLException;

  /**
   * Creates a savepoint in the current transaction.
   *
   * @return the new savepoint
   * @throws SQLException if a database access error occurs
   */
  Savepoint setSavepoint() throws SQLException;

  /**
   * Releases {@code savepoint}.
   *
   * @param savepoint the savepoint to release
   * @throws SQLException if a database access error occurs
   */
  void releaseSavepoint(Savepoint savepoint) throws SQLException;

  /**
   * Wraps {@code connection} in a {@code TransactionContext} whose members all delegate straight
   * through to it.
   *
   * @param connection the JDBC connection to wrap
   * @return a {@code TransactionContext} backed by {@code connection}
   */
  static TransactionContext of(Connection connection) {
    Objects.requireNonNull(connection, "connection");
    return new TransactionContext() {
      @Override
      public <R> R execute(Statement<R> statement) throws SQLException {
        return statement.execute(connection);
      }

      @Override
      public <R> ArrayList<R> execute(StatementBatch<R> batch) throws SQLException {
        return batch.execute(connection);
      }

      @Override
      public boolean getAutoCommit() throws SQLException {
        return connection.getAutoCommit();
      }

      @Override
      public void setAutoCommit(boolean autoCommit) throws SQLException {
        connection.setAutoCommit(autoCommit);
      }

      @Override
      public int getTransactionIsolation() throws SQLException {
        return connection.getTransactionIsolation();
      }

      @Override
      public void setTransactionIsolation(int level) throws SQLException {
        connection.setTransactionIsolation(level);
      }

      @Override
      public boolean isReadOnly() throws SQLException {
        return connection.isReadOnly();
      }

      @Override
      public void setReadOnly(boolean readOnly) throws SQLException {
        connection.setReadOnly(readOnly);
      }

      @Override
      public void commit() throws SQLException {
        connection.commit();
      }

      @Override
      public void rollback() throws SQLException {
        connection.rollback();
      }

      @Override
      public void rollback(Savepoint savepoint) throws SQLException {
        connection.rollback(savepoint);
      }

      @Override
      public Savepoint setSavepoint() throws SQLException {
        return connection.setSavepoint();
      }

      @Override
      public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        connection.releaseSavepoint(savepoint);
      }
    };
  }
}
