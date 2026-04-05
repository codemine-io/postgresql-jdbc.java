package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;

final class TimestampCodec implements Codec<LocalDateTime> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<LocalDateTime> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.TIMESTAMP;
  }

  @Override
  public void bind(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.TIMESTAMP);
    } else {
      ps.setTimestamp(index, java.sql.Timestamp.valueOf(value));
    }
  }

  @Override
  public LocalDateTime decodeNonNullable(ResultSet rs, int row, int col) throws SQLException {
    java.sql.Timestamp value = rs.getTimestamp(col);
    if (value == null) {
      throw new SQLException("Unexpected NULL value at row " + row + ", column " + col, "22004");
    }
    return value.toLocalDateTime();
  }

  @Override
  public LocalDateTime decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    java.sql.Timestamp value = rs.getTimestamp(col);
    if (value == null) {
      return null;
    }
    return value.toLocalDateTime();
  }
}
