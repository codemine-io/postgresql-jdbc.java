package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

final class BpcharCodec implements Codec<String> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<String> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.BPCHAR;
  }

  @Override
  public void bind(PreparedStatement ps, int index, String value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.CHAR);
    } else {
      ps.setString(index, value);
    }
  }

  @Override
  public String decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    return rs.getString(col);
  }
}
