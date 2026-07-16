package io.codemine.java.postgresql.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

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

  /**
   * Transforms this statement's result type, leaving the SQL, parameter binding and metadata
   * unchanged.
   *
   * @param mapper the function to apply to this statement's decoded result
   * @param <R2> the transformed result type
   * @return a statement producing the mapped result
   */
  default <R2> Statement<R2> map(Function<? super R, ? extends R2> mapper) {
    Objects.requireNonNull(mapper, "mapper");
    Statement<R> self = this;
    return new Statement<R2>() {
      @Override
      public String sql() {
        return self.sql();
      }

      @Override
      public void bindParams(PreparedStatement ps) throws SQLException {
        self.bindParams(ps);
      }

      @Override
      public boolean returnsRows() {
        return self.returnsRows();
      }

      @Override
      public R2 decodeResultSet(ResultSet rs) throws SQLException {
        return mapper.apply(self.decodeResultSet(rs));
      }

      @Override
      public R2 decodeAffectedRows(long affectedRows) throws SQLException {
        return mapper.apply(self.decodeAffectedRows(affectedRows));
      }

      @Override
      public String statementName() {
        return self.statementName();
      }

      @Override
      public Optional<String> operationName() {
        return self.operationName();
      }

      @Override
      public Optional<String> collectionName() {
        return self.collectionName();
      }

      @Override
      public boolean idempotent() {
        return self.idempotent();
      }
    };
  }

  // ---------------------------------------------------------------------------
  // Metadata — optional defaults
  // ---------------------------------------------------------------------------

  /**
   * A human-readable name for this statement uniquely identifying the statement among other ones
   * for this DB. It can be used for span names and logging.
   *
   * <p>By default, it returns the simple class name of the implementing class.
   *
   * @return the statement name, never {@code null}
   */
  default String statementName() {
    return getClass().getSimpleName();
  }

  /**
   * The database operation name, e.g. {@code "INSERT"}, {@code "UPDATE"}, {@code "SELECT"} or
   * {@code "DELETE"}. Empty if unknown.
   *
   * @return the operation name, or empty if not applicable
   */
  default Optional<String> operationName() {
    return Optional.empty();
  }

  /**
   * The primary collection (table) name the operation targets, e.g. {@code "albums"}. Empty if
   * unknown.
   *
   * @return the collection name, or empty if not applicable
   */
  default Optional<String> collectionName() {
    return Optional.empty();
  }

  /**
   * Whether this statement is idempotent, i.e. safe to retry after a failure without risking
   * duplicated effects (e.g. a {@code SELECT} or an {@code UPSERT}).
   *
   * @return {@code true} if the statement is idempotent; {@code false} by default
   */
  default boolean idempotent() {
    return false;
  }
}
