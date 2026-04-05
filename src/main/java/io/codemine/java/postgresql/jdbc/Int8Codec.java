package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

final class Int8Codec implements Codec<Long> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<Long> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.INT8;
  }

  @Override
  public void bind(PreparedStatement ps, int index, Long value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.BIGINT);
    } else {
      ps.setLong(index, value);
    }
  }

  @Override
  public Long decodeNonNullable(ResultSet rs, int row, int col) throws SQLException {
    long value = rs.getLong(col);
    if (rs.wasNull()) {
      throw new SQLException("Unexpected NULL value at row " + row + ", column " + col, "22004");
    }
    return value;
  }

  @Override
  public Long decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    long value = rs.getLong(col);
    if (rs.wasNull()) {
      return null;
    }
    return value;
  }
}
