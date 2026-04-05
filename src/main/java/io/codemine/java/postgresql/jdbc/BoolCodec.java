package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

final class BoolCodec implements Codec<Boolean> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<Boolean> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.BOOL;
  }

  @Override
  public void bind(PreparedStatement ps, int index, Boolean value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.BOOLEAN);
    } else {
      ps.setBoolean(index, value);
    }
  }

  @Override
  public Boolean decodeNonNullable(ResultSet rs, int row, int col) throws SQLException {
    boolean value = rs.getBoolean(col);
    if (rs.wasNull()) {
      throw new SQLException("Unexpected NULL value at row " + row + ", column " + col, "22004");
    }
    return value;
  }

  @Override
  public Boolean decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    boolean value = rs.getBoolean(col);
    if (rs.wasNull()) {
      return null;
    }
    return value;
  }
}
