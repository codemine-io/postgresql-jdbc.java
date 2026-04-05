package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

final class Float4Codec implements Codec<Float> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<Float> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.FLOAT4;
  }

  @Override
  public void bind(PreparedStatement ps, int index, Float value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.REAL);
    } else {
      ps.setFloat(index, value);
    }
  }

  @Override
  public Float decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    float value = rs.getFloat(col);
    if (rs.wasNull()) {
      return null;
    }
    return value;
  }
}
