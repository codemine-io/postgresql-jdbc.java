package io.codemine.java.postgresql.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * Helper for executing statements in batches. All statements must be of the same type (i.e. have
 * the same SQL text and result type) and must not return rows.
 */
public final class StatementBatch<R> {

  /**
   * Convenience varargs factory method.
   *
   * @param statements the statements to execute in batch
   * @param <T> the result type
   * @return a new {@code StatementBatch} containing the provided statements
   */
  @SafeVarargs
  public static <T> StatementBatch<T> of(Statement<T>... statements) {
    return new StatementBatch<>(Arrays.asList(statements));
  }

  private final ArrayList<Statement<R>> statements;
  private final String sql;

  /**
   * Create a batch of statements to execute together. All statements must be of the same type (i.e.
   * have the same SQL text and result type) and must not return rows.
   *
   * @param statements the statements to execute in batch
   */
  public StatementBatch(Iterable<? extends Statement<R>> statements) {
    Objects.requireNonNull(statements, "statements");

    this.statements = new ArrayList<>();
    String batchSql = null;

    for (Statement<R> statement : statements) {
      Statement<R> batchStatement = Objects.requireNonNull(statement, "statement");
      if (batchStatement.returnsRows()) {
        throw new IllegalArgumentException(
            "Batch execution is only supported for update statements");
      }

      String statementSql = Objects.requireNonNull(batchStatement.sql(), "sql");
      if (batchSql == null) {
        batchSql = statementSql;
      } else if (!batchSql.equals(statementSql)) {
        throw new IllegalArgumentException("All batch statements must use the same SQL text");
      }

      this.statements.add(batchStatement);
    }

    this.sql = batchSql;
  }

  /**
   * Execute the batch of statements using the provided JDBC connection. Returns a list of decoded
   * affected-row results, in the same order as the input statements.
   *
   * @param connection the JDBC connection to use for batch execution
   * @return a list of decoded results corresponding to each statement in the batch
   * @throws SQLException if a database access error occurs during execution
   */
  public ArrayList<R> execute(Connection connection) throws SQLException {
    Objects.requireNonNull(connection, "connection");

    if (statements.isEmpty()) {
      return new ArrayList<>();
    }

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      for (Statement<R> statement : statements) {
        ps.clearParameters();
        statement.bindParams(ps);
        ps.addBatch();
      }

      int[] affectedRows = ps.executeBatch();
      ArrayList<R> results = new ArrayList<>(affectedRows.length);
      for (int index = 0; index < affectedRows.length; index++) {
        results.add(statements.get(index).decodeAffectedRows(affectedRows[index]));
      }
      return results;
    }
  }
}
