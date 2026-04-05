package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

final class CharCodec implements Codec<Byte> {
  @Override
  public io.codemine.java.postgresql.codecs.Codec<Byte> toAgnostic() {
    return io.codemine.java.postgresql.codecs.Codec.CHAR;
  }

  @Override
  public void bind(PreparedStatement ps, int index, Byte value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.OTHER);
    } else {
      // pgjdbc has no native binding for PostgreSQL's internal "char" (OID 18) type.
      // Encode the byte as a single ISO-8859-1 character; PostgreSQL can cast varchar
      // to "char" by taking the first byte.
      ps.setString(index, String.valueOf((char) (value & 0xFF)));
    }
  }

  @Override
  public Byte decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    // rs.getByte() cannot parse character strings; read as String and extract first byte.
    String s = rs.getString(col);
    if (s == null) {
      return null;
    }
    return s.isEmpty() ? 0 : (byte) s.charAt(0);
  }
}
