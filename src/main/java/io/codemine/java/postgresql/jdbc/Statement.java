package io.codemine.java.postgresql.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Implemented by each query's parameter+result class. Provides a uniform way to prepare and execute
 * statements against a JDBC {@link java.sql.Connection}.
 *
 * @param <R> the result type returned by {@link #decodeResultSet} or {@link #decodeAffectedRows}
 */
public interface Statement<R> {

  /**
   * The SQL text for this statement. Parameter placeholders use JDBC {@code ?} syntax; custom
   * PostgreSQL types are cast explicitly, e.g. {@code ?::album_format}.
   *
   * @return the SQL text for this statement
   */
  String sql();

  /**
   * Bind to the prepared statement's parameter slots.
   *
   * @param ps the prepared statement to bind parameters on
   * @throws SQLException if a database access error occurs while binding
   */
  void bindParams(PreparedStatement ps) throws SQLException;

  /**
   * Whether this statement returns rows (i.e. is a {@code SELECT} or contains a {@code RETURNING}
   * clause).
   */
  boolean returnsRows();

  /**
   * Decode a result set into the statement's result type.
   *
   * @param rs the result set positioned before the first row
   * @return the decoded result of type {@code R}
   * @throws SQLException if a database access error occurs while decoding
   */
  R decodeResultSet(ResultSet rs) throws SQLException;

  /**
   * Decode an affected-row count into the statement's result type.
   *
   * @param affectedRows the number of rows affected
   * @return the decoded result of type {@code R}
   * @throws SQLException if a database access error occurs while decoding
   */
  R decodeAffectedRows(long affectedRows) throws SQLException;

  /**
   * Returns the ordered list of codecs for this statement's bind parameters, in the same order as
   * the {@code ?} placeholders in {@link #sql()}.
   *
   * <p>The default implementation returns an empty list, meaning no parameter compatibility check
   * is performed. Override this method to participate in {@link
   * #checkCompatibility(java.sql.Connection)}.
   *
   * @return an ordered list of parameter codecs
   */
  default List<Codec<?>> paramCodecs() {
    return List.of();
  }

  /**
   * Returns the ordered list of codecs for this statement's result columns, in the same order as
   * the columns returned by the result set.
   *
   * <p>The default implementation returns an empty list, meaning no result compatibility check is
   * performed. Override this method to participate in {@link
   * #checkCompatibility(java.sql.Connection)}.
   *
   * @return an ordered list of result column codecs
   */
  default List<Codec<?>> resultCodecs() {
    return List.of();
  }

  /**
   * Checks whether this statement is compatible with the current database schema using a
   * side-effect-free EXPLAIN-backed probe.
   *
   * <p>The check issues {@code EXPLAIN (VERBOSE, FORMAT JSON)} with the same bind parameters to
   * validate that the statement is still parseable and plannable against the current schema.
   * Parameter and result types inferred by PostgreSQL are then compared against the codecs declared
   * by {@link #paramCodecs()} and {@link #resultCodecs()} using assignment-compatible PostgreSQL
   * type rules.
   *
   * <p>This method is intended for use in startup or liveness probes that need to detect schema
   * drift without executing the statement for real.
   *
   * @param conn the JDBC connection to use for the check
   * @return a {@link StatementCompatibilityReport} describing any mismatches
   * @throws SQLException if a database access error occurs while checking
   */
  default StatementCompatibilityReport checkCompatibility(Connection conn) throws SQLException {
    return StatementCompatibilityChecker.check(this, conn);
  }

  /**
   * Execute this statement using the provided JDBC connection.
   *
   * @param conn the JDBC connection to use
   * @return the decoded statement result of type {@code R}
   * @throws SQLException if a database access error occurs while executing the statement
   */
  default R execute(Connection conn) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(sql())) {
      bindParams(ps);
      if (returnsRows()) {
        ps.execute();
        try (ResultSet rs = ps.getResultSet()) {
          return decodeResultSet(rs);
        }
      } else {
        long affectedRows = ps.executeUpdate();
        return decodeAffectedRows(affectedRows);
      }
    }
  }
}
