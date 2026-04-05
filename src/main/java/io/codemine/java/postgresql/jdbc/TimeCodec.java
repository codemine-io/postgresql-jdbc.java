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
      // Use setObject with LocalTime directly: pgjdbc supports Java 8 time types natively
      // and preserves sub-second (microsecond) precision, unlike java.sql.Time.valueOf().
      ps.setObject(index, value);
    }
  }

  @Override
  public LocalTime decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    // getObject(col, LocalTime.class) preserves microseconds; rs.getTime() truncates to seconds.
    return rs.getObject(col, LocalTime.class);
  }
}
