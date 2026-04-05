package io.codemine.java.postgresql.jdbc;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

final class NumericCodec implements Codec<BigDecimal> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<BigDecimal> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.NUMERIC;
  }

  @Override
  public void bind(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.NUMERIC);
    } else {
      ps.setBigDecimal(index, value);
    }
  }

  @Override
  public BigDecimal decodeNonNullable(ResultSet rs, int row, int col) throws SQLException {
    BigDecimal value = rs.getBigDecimal(col);
    if (value == null) {
      throw new SQLException("Unexpected NULL value at row " + row + ", column " + col, "22004");
    }
    return value;
  }

  @Override
  public BigDecimal decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    return rs.getBigDecimal(col);
  }
}
