package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.postgresql.util.PGobject;

/**
 * A codec for PostgreSQL types intended for use with pgJDBC. This class provides methods for
 * binding values to prepared statements and decoding values from result sets, using the underlying
 * * {@link Codec} for the actual encoding and decoding logic.
 */
final class AgnosticCodec<A> implements Codec<A> {

  private final io.codemine.java.postgresql.codecs.Codec<A> codec;

  /**
   * Creates a new {@link JdbcCodec} instance.
   *
   * @param codec the underlying codec
   */
  public AgnosticCodec(io.codemine.java.postgresql.codecs.Codec<A> codec) {
    this.codec = codec;
  }

  /**
   * Returns the underlying codec.
   *
   * @return the underlying codec
   */
  public io.codemine.java.postgresql.codecs.Codec<A> toAgnostic() {
    return codec;
  }

  /**
   * Binds a value to a prepared statement.
   *
   * @param ps the prepared statement
   * @param index the parameter index
   * @param value the value to bind
   * @throws SQLException if a database access error occurs
   */
  @Override
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
  @Override
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
  @Override
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
  @Override
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
