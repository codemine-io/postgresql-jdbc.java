package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

final class CharCodec implements Codec<Byte> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<Byte> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.CHAR;
  }

  @Override
  public void bind(PreparedStatement ps, int index, Byte value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.TINYINT);
    } else {
      ps.setByte(index, value);
    }
  }

  @Override
  public Byte decodeNonNullable(ResultSet rs, int row, int col) throws SQLException {
    byte value = rs.getByte(col);
    if (rs.wasNull()) {
      throw new SQLException("Unexpected NULL value at row " + row + ", column " + col, "22004");
    }
    return value;
  }

  @Override
  public Byte decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    byte value = rs.getByte(col);
    if (rs.wasNull()) {
      return null;
    }
    return value;
  }
}
