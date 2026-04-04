package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public interface Codec<A> {
  /**
   * Binds a value to a prepared statement.
   *
   * @param ps the prepared statement
   * @param index the parameter index
   * @param value the value to bind
   * @throws SQLException if a database access error occurs
   */
  public void bind(PreparedStatement ps, int index, A value) throws SQLException;

  /**
   * Decodes a non-nullable value from the result set.
   *
   * @param rs the result set
   * @param row the row index
   * @param col the column index
   * @return the decoded value
   * @throws SQLException if a database access error occurs or the value is null
   */
  public A decodeNonNullable(ResultSet rs, int row, int col) throws SQLException;

  /**
   * Decodes a nullable value from the result set.
   *
   * @param rs the result set
   * @param row the row index
   * @param col the column index
   * @return the decoded value, or null if the value is SQL NULL
   * @throws SQLException if a database access error occurs
   */
  public A decodeNullable(ResultSet rs, int row, int col) throws SQLException;

  /**
   * Decodes an optional value from the result set.
   *
   * @param rs the result set
   * @param row the row index
   * @param col the column index
   * @return the decoded value wrapped in an {@link Optional}, or {@link Optional#empty()} if the
   *     value is SQL NULL
   * @throws SQLException if a database access error occurs
   */
  default Optional<A> decodeOptional(ResultSet rs, int row, int col) throws SQLException {
    return Optional.ofNullable(decodeNullable(rs, row, col));
  }
}
