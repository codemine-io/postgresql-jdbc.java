package io.codemine.java.postgresql.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Helper for executing statements as a single JDBC batch. All statements must share the same SQL
 * text and must not return rows.
 */
public final class StatementBatch<R> {
  private final List<Statement<R>> statements;
  private final String sql;

  /**
   * Create a batch of statements to execute together. All statements must be of the same type (i.e.
   * have the same SQL text and result type) and must not return rows.
   *
   * @param statements the statements to execute in batch
   */
  public StatementBatch(Iterable<? extends Statement<R>> statements) {
    Objects.requireNonNull(statements, "statements");

    List<Statement<R>> batch = new ArrayList<>();
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

      batch.add(batchStatement);
    }

    this.statements = batch;
    this.sql = batchSql;
  }

  /**
   * The shared SQL text of the batch, or {@code null} if the batch is empty.
   *
   * @return the shared SQL text of the batch, or {@code null} if empty
   */
  public String sql() {
    return sql;
  }

  /**
   * The number of statements in the batch.
   *
   * @return the number of statements in the batch
   */
  public int size() {
    return statements.size();
  }

  /**
   * Execute the batch of statements using the provided JDBC connection. Returns a list of decoded
   * affected-row results, in the same order as the input statements.
   *
   * @param connection the JDBC connection to use for batch execution
   * @return a list of decoded results corresponding to each statement in the batch
   * @throws SQLException if a database access error occurs during execution
   */
  public List<R> execute(Connection connection) throws SQLException {
    return execute(connection, 0);
  }

  /**
   * Execute the batch of statements using the provided JDBC connection, bounding each attempt by
   * the given query timeout. Returns a list of decoded affected-row results, in the same order as
   * the input statements.
   *
   * @param connection the JDBC connection to use for batch execution
   * @param queryTimeoutSeconds the query timeout to apply, in seconds; values less than or equal to
   *     0 leave the driver's default in place
   * @return a list of decoded results corresponding to each statement in the batch
   * @throws SQLException if a database access error occurs during execution
   */
  public List<R> execute(Connection connection, int queryTimeoutSeconds) throws SQLException {
    Objects.requireNonNull(connection, "connection");

    if (statements.isEmpty()) {
      return List.of();
    }

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      if (queryTimeoutSeconds > 0) {
        ps.setQueryTimeout(queryTimeoutSeconds);
      }
      for (Statement<R> statement : statements) {
        ps.clearParameters();
        statement.bindParams(ps);
        ps.addBatch();
      }

      int[] affectedRows = ps.executeBatch();
      List<R> results = new ArrayList<>(affectedRows.length);
      for (int index = 0; index < affectedRows.length; index++) {
        results.add(statements.get(index).decodeAffectedRows(affectedRows[index]));
      }
      return results;
    }
  }
}
