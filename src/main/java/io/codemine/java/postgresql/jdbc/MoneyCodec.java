package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.postgresql.util.PGobject;

final class MoneyCodec implements Codec<Long> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<Long> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.MONEY;
  }

  @Override
  public void bind(PreparedStatement ps, int index, Long value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.OTHER);
    } else {
      // The agnostic codec represents money as cents (e.g. 1957L = $19.57).
      // Encode as a decimal string (e.g. "19.57") so PostgreSQL parses it correctly,
      // regardless of the server's lc_monetary locale setting.
      PGobject obj = new PGobject();
      obj.setType("money");
      obj.setValue(toAgnostic().encodeInTextToString(value));
      ps.setObject(index, obj);
    }
  }

  @Override
  public Long decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    // rs.getLong() cannot parse locale-specific money strings like "$1,234.56".
    // Delegate to the agnostic codec's text decoder which handles all money formats.
    String text = rs.getString(col);
    if (text == null) {
      return null;
    }
    try {
      return toAgnostic().decodeInTextFromString(text);
    } catch (io.codemine.java.postgresql.codecs.Codec.DecodingException e) {
      throw new SQLException("Failed to decode money value: " + text, "22000", e);
    }
  }
}
