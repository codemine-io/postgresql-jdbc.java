package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

final class Int2Codec implements Codec<Short> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<Short> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.INT2;
  }

  @Override
  public void bind(PreparedStatement ps, int index, Short value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.SMALLINT);
    } else {
      ps.setShort(index, value);
    }
  }

  @Override
  public Short decodeNonNullable(ResultSet rs, int row, int col) throws SQLException {
    short value = rs.getShort(col);
    if (rs.wasNull()) {
      throw new SQLException("Unexpected NULL value at row " + row + ", column " + col, "22004");
    }
    return value;
  }

  @Override
  public Short decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    short value = rs.getShort(col);
    if (rs.wasNull()) {
      return null;
    }
    return value;
  }
}
