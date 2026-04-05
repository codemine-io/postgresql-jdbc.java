package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class Int4Codec implements Codec<Integer> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<Integer> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.INT4;
  }

  @Override
  public void bind(PreparedStatement ps, int index, Integer value) throws SQLException {
    if (value == null) {
      ps.setNull(index, java.sql.Types.INTEGER);
    } else {
      ps.setInt(index, value);
    }
  }

  @Override
  public Integer decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    int value = rs.getInt(col);
    if (rs.wasNull()) {
      return null;
    }
    return value;
  }
}
