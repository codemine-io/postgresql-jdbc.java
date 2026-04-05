package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalTime;

final class TimeCodec implements Codec<LocalTime> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<LocalTime> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.TIME;
  }

  @Override
  public void bind(PreparedStatement ps, int index, LocalTime value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.TIME);
    } else {
      ps.setTime(index, java.sql.Time.valueOf(value));
    }
  }

  @Override
  public LocalTime decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    java.sql.Time value = rs.getTime(col);
    if (value == null) {
      return null;
    }
    return value.toLocalTime();
  }
}
