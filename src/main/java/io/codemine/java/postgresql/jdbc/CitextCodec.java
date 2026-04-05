package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

final class CitextCodec implements Codec<String> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<String> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.CITEXT;
  }

  @Override
  public void bind(PreparedStatement ps, int index, String value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.VARCHAR);
    } else {
      ps.setString(index, value);
    }
  }

  @Override
  public String decodeNonNullable(ResultSet rs, int row, int col) throws SQLException {
    String value = rs.getString(col);
    if (value == null) {
      throw new SQLException("Unexpected NULL value at row " + row + ", column " + col, "22004");
    }
    return value;
  }

  @Override
  public String decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    return rs.getString(col);
  }
}
