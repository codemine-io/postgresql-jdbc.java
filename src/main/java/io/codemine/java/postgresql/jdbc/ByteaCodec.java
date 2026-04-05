package io.codemine.java.postgresql.jdbc;

import io.codemine.java.postgresql.codecs.Bytea;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

final class ByteaCodec implements Codec<Bytea> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<Bytea> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.BYTEA;
  }

  @Override
  public void bind(PreparedStatement ps, int index, Bytea value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.VARBINARY);
    } else {
      ps.setBytes(index, value.bytes());
    }
  }

  @Override
  public Bytea decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    byte[] value = rs.getBytes(col);
    if (value == null) {
      return null;
    }
    return new Bytea(value);
  }
}
