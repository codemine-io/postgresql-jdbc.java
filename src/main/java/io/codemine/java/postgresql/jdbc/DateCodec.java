package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

final class DateCodec implements Codec<LocalDate> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<LocalDate> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.DATE;
  }

  @Override
  public void bind(PreparedStatement ps, int index, LocalDate value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.DATE);
    } else {
      ps.setDate(index, java.sql.Date.valueOf(value));
    }
  }

  @Override
  public LocalDate decodeNonNullable(ResultSet rs, int row, int col) throws SQLException {
    java.sql.Date value = rs.getDate(col);
    if (value == null) {
      throw new SQLException("Unexpected NULL value at row " + row + ", column " + col, "22004");
    }
    return value.toLocalDate();
  }

  @Override
  public LocalDate decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    java.sql.Date value = rs.getDate(col);
    if (value == null) {
      return null;
    }
    return value.toLocalDate();
  }
}
