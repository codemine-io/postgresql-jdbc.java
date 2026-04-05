package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

final class UuidCodec implements Codec<UUID> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<UUID> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.UUID;
  }

  @Override
  public void bind(PreparedStatement ps, int index, UUID value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.OTHER);
    } else {
      ps.setObject(index, value);
    }
  }

  @Override
  public UUID decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    return rs.getObject(col, UUID.class);
  }
}
