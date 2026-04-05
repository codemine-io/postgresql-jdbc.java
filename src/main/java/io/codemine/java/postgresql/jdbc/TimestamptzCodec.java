package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class TimestamptzCodec implements Codec<Instant> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<Instant> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.TIMESTAMPTZ;
  }

  @Override
  public void bind(PreparedStatement ps, int index, Instant value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.TIMESTAMP_WITH_TIMEZONE);
    } else {
      ps.setObject(index, value.atOffset(ZoneOffset.UTC));
    }
  }

  @Override
  public Instant decodeNonNullable(ResultSet rs, int row, int col) throws SQLException {
    OffsetDateTime value = rs.getObject(col, OffsetDateTime.class);
    if (value == null) {
      throw new SQLException("Unexpected NULL value at row " + row + ", column " + col, "22004");
    }
    return value.toInstant();
  }

  @Override
  public Instant decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    OffsetDateTime value = rs.getObject(col, OffsetDateTime.class);
    if (value == null) {
      return null;
    }
    return value.toInstant();
  }
}
