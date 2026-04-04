package io.codemine.java.postgresql.jdbc;

import com.fasterxml.jackson.databind.JsonNode;
import io.codemine.java.postgresql.codecs.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.postgresql.util.PGobject;

/**
 * A codec for PostgreSQL types intended for use with pgJDBC. This class provides methods for
 * binding values to prepared statements and decoding values from result sets, using the underlying
 * * {@link Codec} for the actual encoding and decoding logic.
 */
public final class JdbcCodec<A> {

  public static final JdbcCodec<Integer> INT4 = new JdbcCodec<>(Codec.INT4);
  public static final JdbcCodec<String> TEXT = new JdbcCodec<>(Codec.TEXT);
  public static final JdbcCodec<Inet> INET = new JdbcCodec<>(Codec.INET);
  public static final JdbcCodec<Macaddr> MACADDR = new JdbcCodec<>(Codec.MACADDR);
  public static final JdbcCodec<Boolean> BOOL = new JdbcCodec<>(Codec.BOOL);
  public static final JdbcCodec<Short> INT2 = new JdbcCodec<>(Codec.INT2);
  public static final JdbcCodec<Long> INT8 = new JdbcCodec<>(Codec.INT8);
  public static final JdbcCodec<Float> FLOAT4 = new JdbcCodec<>(Codec.FLOAT4);
  public static final JdbcCodec<Double> FLOAT8 = new JdbcCodec<>(Codec.FLOAT8);
  public static final JdbcCodec<java.math.BigDecimal> NUMERIC = new JdbcCodec<>(Codec.NUMERIC);
  public static final JdbcCodec<Bytea> BYTEA = new JdbcCodec<>(Codec.BYTEA);
  public static final JdbcCodec<java.util.UUID> UUID = new JdbcCodec<>(Codec.UUID);
  public static final JdbcCodec<JsonNode> JSON = new JdbcCodec<>(Codec.JSON);
  public static final JdbcCodec<JsonNode> JSONB = new JdbcCodec<>(Codec.JSONB);
  public static final JdbcCodec<String> VARCHAR = new JdbcCodec<>(Codec.VARCHAR);
  public static final JdbcCodec<String> BPCHAR = new JdbcCodec<>(Codec.BPCHAR);
  public static final JdbcCodec<Byte> CHAR = new JdbcCodec<>(Codec.CHAR);
  public static final JdbcCodec<Integer> OID = new JdbcCodec<>(Codec.OID);
  public static final JdbcCodec<Long> MONEY = new JdbcCodec<>(Codec.MONEY);
  public static final JdbcCodec<java.time.LocalDate> DATE = new JdbcCodec<>(Codec.DATE);
  public static final JdbcCodec<java.time.LocalTime> TIME = new JdbcCodec<>(Codec.TIME);
  public static final JdbcCodec<Timetz> TIMETZ = new JdbcCodec<>(Codec.TIMETZ);
  public static final JdbcCodec<java.time.LocalDateTime> TIMESTAMP =
      new JdbcCodec<>(Codec.TIMESTAMP);
  public static final JdbcCodec<java.time.Instant> TIMESTAMPTZ = new JdbcCodec<>(Codec.TIMESTAMPTZ);
  public static final JdbcCodec<Interval> INTERVAL = new JdbcCodec<>(Codec.INTERVAL);
  public static final JdbcCodec<Point> POINT = new JdbcCodec<>(Codec.POINT);
  public static final JdbcCodec<Line> LINE = new JdbcCodec<>(Codec.LINE);
  public static final JdbcCodec<Lseg> LSEG = new JdbcCodec<>(Codec.LSEG);
  public static final JdbcCodec<Box> BOX = new JdbcCodec<>(Codec.BOX);
  public static final JdbcCodec<Path> PATH = new JdbcCodec<>(Codec.PATH);
  public static final JdbcCodec<Polygon> POLYGON = new JdbcCodec<>(Codec.POLYGON);
  public static final JdbcCodec<Circle> CIRCLE = new JdbcCodec<>(Codec.CIRCLE);
  public static final JdbcCodec<Cidr> CIDR = new JdbcCodec<>(Codec.CIDR);
  public static final JdbcCodec<Macaddr8> MACADDR8 = new JdbcCodec<>(Codec.MACADDR8);
  public static final JdbcCodec<Bit> BIT = new JdbcCodec<>(Codec.BIT);
  public static final JdbcCodec<Bit> VARBIT = new JdbcCodec<>(Codec.VARBIT);
  public static final JdbcCodec<String> CITEXT = new JdbcCodec<>(Codec.CITEXT);
  public static final JdbcCodec<Tsvector> TSVECTOR = new JdbcCodec<>(Codec.TSVECTOR);
  public static final JdbcCodec<Hstore> HSTORE = new JdbcCodec<>(Codec.HSTORE);
  public static final JdbcCodec<Range<Integer>> INT4RANGE = new JdbcCodec<>(Codec.INT4RANGE);
  public static final JdbcCodec<Range<Long>> INT8RANGE = new JdbcCodec<>(Codec.INT8RANGE);
  public static final JdbcCodec<Range<java.math.BigDecimal>> NUMRANGE =
      new JdbcCodec<>(Codec.NUMRANGE);
  public static final JdbcCodec<Range<java.time.LocalDateTime>> TSRANGE =
      new JdbcCodec<>(Codec.TSRANGE);
  public static final JdbcCodec<Range<java.time.Instant>> TSTZRANGE =
      new JdbcCodec<>(Codec.TSTZRANGE);
  public static final JdbcCodec<Range<java.time.LocalDate>> DATERANGE =
      new JdbcCodec<>(Codec.DATERANGE);
  public static final JdbcCodec<Multirange<Integer>> INT4MULTIRANGE =
      new JdbcCodec<>(Codec.INT4MULTIRANGE);
  public static final JdbcCodec<Multirange<Long>> INT8MULTIRANGE =
      new JdbcCodec<>(Codec.INT8MULTIRANGE);
  public static final JdbcCodec<Multirange<java.math.BigDecimal>> NUMMULTIRANGE =
      new JdbcCodec<>(Codec.NUMMULTIRANGE);
  public static final JdbcCodec<Multirange<java.time.LocalDateTime>> TSMULTIRANGE =
      new JdbcCodec<>(Codec.TSMULTIRANGE);
  public static final JdbcCodec<Multirange<java.time.Instant>> TSTZMULTIRANGE =
      new JdbcCodec<>(Codec.TSTZMULTIRANGE);
  public static final JdbcCodec<Multirange<java.time.LocalDate>> DATEMULTIRANGE =
      new JdbcCodec<>(Codec.DATEMULTIRANGE);

  /**
   * Returns a codec for PostgreSQL {@code bit(n)} — a fixed-length bit string of exactly {@code n}
   * bits.
   *
   * <p>If {@code n <= 0}, this returns the unparameterized {@link #BIT} codec.
   */
  public static JdbcCodec<Bit> bit(int n) {
    return new JdbcCodec<>(Codec.bit(n));
  }

  /**
   * Returns a codec for PostgreSQL {@code varbit(n)} — a variable-length bit string of at most
   * {@code n} bits.
   *
   * <p>If {@code n <= 0}, this returns the unparameterized {@link #VARBIT} codec.
   */
  public static JdbcCodec<Bit> varbit(int n) {
    return new JdbcCodec<>(Codec.varbit(n));
  }

  /**
   * Returns a codec for PostgreSQL {@code varchar(n)} — a variable-length character string of at
   * most {@code n} characters.
   *
   * <p>If {@code n <= 0}, this returns the unparameterized {@link #VARCHAR} codec.
   */
  public static JdbcCodec<String> varchar(int n) {
    return new JdbcCodec<>(Codec.varchar(n));
  }

  /**
   * Returns a codec for PostgreSQL {@code bpchar(n)} — a fixed-length blank-padded character string
   * of exactly {@code n} characters.
   */
  public static JdbcCodec<String> bpchar(int n) {
    return new JdbcCodec<>(Codec.bpchar(n));
  }

  /**
   * Returns a codec for PostgreSQL enum types.
   *
   * @param schema the schema name. Empty string or null for the default schema (usually "public")
   * @param name the enum type name
   * @param valueToLabel a map of enum values to their corresponding labels
   * @param <A> the enum type
   * @return a {@link JdbcCodec} for the specified enum type
   */
  public static <A> JdbcCodec<A> enum_(String schema, String name, Map<A, String> valueToLabel) {
    return new JdbcCodec<>(new EnumCodec<>(schema, name, valueToLabel));
  }

  /**
   * Creates a composite codec for any number of fields using an untyped vararg array constructor.
   *
   * <p>The {@code construct} function receives an {@code Object[]} whose elements correspond
   * positionally to the supplied field descriptors.
   *
   * <p><b>Note:</b> this constructor is less safely typed than the arity-specific overloads.
   * Callers are responsible for casting elements of the array to the correct types.
   *
   * @param schema PostgreSQL schema name, or empty/null for default search path
   * @param name PostgreSQL composite type name
   * @param construct function that maps an {@code Object[]} of decoded field values to {@code Z}
   * @param fields field descriptors in declaration order
   */
  @SafeVarargs
  public static <Z> JdbcCodec<Z> composite(
      String schema,
      String name,
      Function<Object[], Z> construct,
      CompositeCodec.Field<Z, ?>... fields) {
    return new JdbcCodec<>(new CompositeCodec<>(schema, name, construct, fields));
  }

  /**
   * Creates a field descriptor for a composite type.
   *
   * @param name the field name
   * @param codec the codec for the field type
   * @param getter a function to extract the field value from the composite object
   * @param <Z> the composite type
   * @param <A> the field type
   * @return a field descriptor
   */
  public static <Z, A> CompositeCodec.Field<Z, A> field(
      String name, JdbcCodec<A> codec, Function<Z, A> getter) {
    return new CompositeCodec.Field<>(name, getter, codec.codec);
  }

  private final Codec<A> codec;

  /**
   * Creates a new {@link JdbcCodec} instance.
   *
   * @param codec the underlying codec
   */
  public JdbcCodec(Codec<A> codec) {
    this.codec = codec;
  }

  /**
   * Binds a value to a prepared statement.
   *
   * @param ps the prepared statement
   * @param index the parameter index
   * @param value the value to bind
   * @throws SQLException if a database access error occurs
   */
  public void bind(PreparedStatement ps, int index, A value) throws SQLException {
    PGobject obj = new PGobject();
    obj.setType(codec.typeSig());
    if (value != null) {
      obj.setValue(codec.encodeInTextToString(value));
    }
    ps.setObject(index, obj);
  }

  /**
   * Decodes a non-nullable value from the result set.
   *
   * @param rs the result set
   * @param row the row index
   * @param col the column index
   * @return the decoded value
   * @throws SQLException if a database access error occurs or the value is null
   */
  public A decodeNonNullable(ResultSet rs, int row, int col) throws SQLException {
    String text = rs.getString(col);
    if (text == null) {
      throw new SQLException("Unexpected NULL value at row " + row + ", column " + col, "22004");
    }
    try {
      return codec.decodeInTextFromString(text);
    } catch (Codec.DecodingException e) {
      throw new SQLException("Failed to decode cell at row " + row + ", column " + col, "22000", e);
    }
  }

  /**
   * Decodes a nullable value from the result set.
   *
   * @param rs the result set
   * @param row the row index
   * @param col the column index
   * @return the decoded value, or null if the value is SQL NULL
   * @throws SQLException if a database access error occurs
   */
  public A decodeNullable(ResultSet rs, int row, int col) throws SQLException {
    String text = rs.getString(col);
    if (text == null) {
      return null;
    }
    try {
      return codec.decodeInTextFromString(text);
    } catch (Codec.DecodingException e) {
      throw new SQLException("Failed to decode cell at row " + row + ", column " + col, "22000", e);
    }
  }

  /**
   * Decodes an optional value from the result set.
   *
   * @param rs the result set
   * @param row the row index
   * @param col the column index
   * @return the decoded value wrapped in an {@link Optional}, or {@link Optional#empty()} if the
   *     value is SQL NULL
   * @throws SQLException if a database access error occurs
   */
  public Optional<A> decodeOptional(ResultSet rs, int row, int col) throws SQLException {
    String text = rs.getString(col);
    if (text == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(codec.decodeInTextFromString(text));
    } catch (Codec.DecodingException e) {
      throw new SQLException("Failed to decode cell at row " + row + ", column " + col, "22000", e);
    }
  }
}
