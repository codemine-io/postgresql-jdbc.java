package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

final class Float8Codec implements Codec<Double> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<Double> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.FLOAT8;
  }

  @Override
  public void bind(PreparedStatement ps, int index, Double value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.DOUBLE);
    } else {
      ps.setDouble(index, value);
    }
  }

  @Override
  public Double decodeNonNullable(ResultSet rs, int row, int col) throws SQLException {
    double value = rs.getDouble(col);
    if (rs.wasNull()) {
      throw new SQLException("Unexpected NULL value at row " + row + ", column " + col, "22004");
    }
    return value;
  }

  @Override
  public Double decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    double value = rs.getDouble(col);
    if (rs.wasNull()) {
      return null;
    }
    return value;
  }
}
