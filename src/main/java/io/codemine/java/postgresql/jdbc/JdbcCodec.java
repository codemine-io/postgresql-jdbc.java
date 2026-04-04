package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.postgresql.util.PGobject;

/**
 * Adapter between the {@code postgresql-codecs} library and JDBC. Provides utilities for encoding
 * and decoding values using {@link Codec} instances and binding them to JDBC statements.
 */
public final class JdbcCodec<A> {

  private final io.codemine.java.postgresql.codecs.Codec<A> codec;

  /**
   * Creates a new {@link JdbcCodec} instance.
   *
   * @param codec the underlying codec
   */
  public JdbcCodec(io.codemine.java.postgresql.codecs.Codec<A> codec) {
    this.codec = codec;
  }

  /**
   * Binds a value to a prepared statement.
   *
   * @param ps the prepared statement
   * @param index the parameter index
   * @param value the value to bind
   * @throws SQLException if a database access error occurs
   */
  public void bind(PreparedStatement ps, int index, A value) throws SQLException {
    PGobject obj = new PGobject();
    obj.setType(codec.typeSig());
    if (value != null) {
      obj.setValue(codec.encodeInTextToString(value));
    }
    ps.setObject(index, obj);
  }

  /**
   * Decodes a non-nullable value from the result set.
   *
   * @param rs the result set
   * @param row the row index
   * @param col the column index
   * @return the decoded value
   * @throws SQLException if a database access error occurs or the value is null
   */
  public A decodeNonNullable(ResultSet rs, int row, int col) throws SQLException {
    String text = rs.getString(col);
    if (text == null) {
      throw new SQLException("Unexpected NULL value at row " + row + ", column " + col, "22004");
    }
    try {
      return codec.decodeInTextFromString(text);
    } catch (io.codemine.java.postgresql.codecs.Codec.DecodingException e) {
      throw new SQLException("Failed to decode cell at row " + row + ", column " + col, "22000", e);
    }
  }

  /**
   * Decodes a nullable value from the result set.
   *
   * @param rs the result set
   * @param row the row index
   * @param col the column index
   * @return the decoded value, or null if the value is SQL NULL
   * @throws SQLException if a database access error occurs
   */
  public A decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    String text = rs.getString(col);
    if (text == null) {
      return null;
    }
    try {
      return codec.decodeInTextFromString(text);
    } catch (io.codemine.java.postgresql.codecs.Codec.DecodingException e) {
      throw new SQLException("Failed to decode cell at row " + row + ", column " + col, "22000", e);
    }
  }

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
  public Optional<A> decodeOptional(ResultSet rs, int row, int col) throws SQLException {
    String text = rs.getString(col);
    if (text == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(codec.decodeInTextFromString(text));
    } catch (io.codemine.java.postgresql.codecs.Codec.DecodingException e) {
      throw new SQLException("Failed to decode cell at row " + row + ", column " + col, "22000", e);
    }
  }
}
