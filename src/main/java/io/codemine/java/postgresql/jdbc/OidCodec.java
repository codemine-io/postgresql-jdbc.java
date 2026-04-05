package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

final class OidCodec implements Codec<Integer> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<Integer> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.OID;
  }

  @Override
  public void bind(PreparedStatement ps, int index, Integer value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.INTEGER);
    } else {
      ps.setInt(index, value);
    }
  }

  @Override
  public Integer decodeNonNullable(ResultSet rs, int row, int col) throws SQLException {
    int value = rs.getInt(col);
    if (rs.wasNull()) {
      throw new SQLException("Unexpected NULL value at row " + row + ", column " + col, "22004");
    }
    return value;
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
