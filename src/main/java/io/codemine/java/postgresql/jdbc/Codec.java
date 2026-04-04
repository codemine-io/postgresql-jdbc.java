package io.codemine.java.postgresql.jdbc;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * A codec for PostgreSQL types intended for use with pgJDBC. This interface provides methods for
 * binding values to prepared statements and decoding values from result sets.
 */
public interface Codec<A> {

  public static final Codec<Integer> INT4 = 
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.INT4);

  public static final Codec<String> TEXT =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.TEXT);

  public static final Codec<io.codemine.java.postgresql.codecs.Inet> INET =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.INET);

  public static final Codec<io.codemine.java.postgresql.codecs.Macaddr> MACADDR =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.MACADDR);

  public static final Codec<Boolean> BOOL =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.BOOL);

  public static final Codec<Short> INT2 =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.INT2);

  public static final Codec<Long> INT8 =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.INT8);

  public static final Codec<Float> FLOAT4 =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.FLOAT4);

  public static final Codec<Double> FLOAT8 =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.FLOAT8);

  public static final Codec<java.math.BigDecimal> NUMERIC =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.NUMERIC);

  public static final Codec<io.codemine.java.postgresql.codecs.Bytea> BYTEA =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.BYTEA);

  public static final Codec<java.util.UUID> UUID =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.UUID);

  public static final Codec<JsonNode> JSON =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.JSON);

  public static final Codec<JsonNode> JSONB =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.JSONB);

  public static final Codec<String> VARCHAR =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.VARCHAR);

  public static final Codec<String> BPCHAR =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.BPCHAR);

  public static final Codec<Byte> CHAR =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.CHAR);

  public static final Codec<Integer> OID =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.OID);

  public static final Codec<Long> MONEY =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.MONEY);

  public static final Codec<java.time.LocalDate> DATE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.DATE);

  public static final Codec<java.time.LocalTime> TIME =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.TIME);

  public static final Codec<io.codemine.java.postgresql.codecs.Timetz> TIMETZ =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.TIMETZ);

  public static final Codec<java.time.LocalDateTime> TIMESTAMP =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.TIMESTAMP);

  public static final Codec<java.time.Instant> TIMESTAMPTZ =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.TIMESTAMPTZ);

  public static final Codec<io.codemine.java.postgresql.codecs.Interval> INTERVAL =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.INTERVAL);

  public static final Codec<io.codemine.java.postgresql.codecs.Point> POINT =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.POINT);

  public static final Codec<io.codemine.java.postgresql.codecs.Line> LINE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.LINE);

  public static final Codec<io.codemine.java.postgresql.codecs.Lseg> LSEG =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.LSEG);

  public static final Codec<io.codemine.java.postgresql.codecs.Box> BOX =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.BOX);

  public static final Codec<io.codemine.java.postgresql.codecs.Path> PATH =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.PATH);

  public static final Codec<io.codemine.java.postgresql.codecs.Polygon> POLYGON =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.POLYGON);

  public static final Codec<io.codemine.java.postgresql.codecs.Circle> CIRCLE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.CIRCLE);

  public static final Codec<io.codemine.java.postgresql.codecs.Cidr> CIDR =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.CIDR);

  public static final Codec<io.codemine.java.postgresql.codecs.Macaddr8> MACADDR8 =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.MACADDR8);

  public static final Codec<io.codemine.java.postgresql.codecs.Bit> BIT =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.BIT);

  public static final Codec<io.codemine.java.postgresql.codecs.Bit> VARBIT =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.VARBIT);

  public static final Codec<String> CITEXT =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.CITEXT);

  public static final Codec<io.codemine.java.postgresql.codecs.Tsvector> TSVECTOR =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.TSVECTOR);

  public static final Codec<io.codemine.java.postgresql.codecs.Hstore> HSTORE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.HSTORE);

  public static final Codec<io.codemine.java.postgresql.codecs.Range<Integer>> INT4RANGE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.INT4RANGE);

  public static final Codec<io.codemine.java.postgresql.codecs.Range<Long>> INT8RANGE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.INT8RANGE);

  public static final Codec<io.codemine.java.postgresql.codecs.Range<java.math.BigDecimal>>
      NUMRANGE = new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.NUMRANGE);

  public static final Codec<io.codemine.java.postgresql.codecs.Range<java.time.LocalDateTime>>
      TSRANGE = new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.TSRANGE);

  public static final Codec<io.codemine.java.postgresql.codecs.Range<java.time.Instant>> TSTZRANGE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.TSTZRANGE);

  public static final Codec<io.codemine.java.postgresql.codecs.Range<java.time.LocalDate>>
      DATERANGE = new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.DATERANGE);

  public static final Codec<io.codemine.java.postgresql.codecs.Multirange<Integer>> INT4MULTIRANGE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.INT4MULTIRANGE);

  public static final Codec<io.codemine.java.postgresql.codecs.Multirange<Long>> INT8MULTIRANGE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.INT8MULTIRANGE);

  public static final Codec<io.codemine.java.postgresql.codecs.Multirange<java.math.BigDecimal>>
      NUMMULTIRANGE = new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.NUMMULTIRANGE);

  public static final Codec<io.codemine.java.postgresql.codecs.Multirange<java.time.LocalDateTime>>
      TSMULTIRANGE = new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.TSMULTIRANGE);

  public static final Codec<io.codemine.java.postgresql.codecs.Multirange<java.time.Instant>>
      TSTZMULTIRANGE = new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.TSTZMULTIRANGE);

  public static final Codec<io.codemine.java.postgresql.codecs.Multirange<java.time.LocalDate>>
      DATEMULTIRANGE = new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.DATEMULTIRANGE);

  /**
   * Returns a codec for PostgreSQL {@code bit(n)} — a fixed-length bit string of exactly {@code n}
   * bits.
   *
   * <p>If {@code n <= 0}, this returns the unparameterized {@link #BIT} codec.
   */
  public static Codec<io.codemine.java.postgresql.codecs.Bit> bit(int n) {
    return new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.bit(n));
  }

  /**
   * Returns a codec for PostgreSQL {@code varbit(n)} — a variable-length bit string of at most
   * {@code n} bits.
   *
   * <p>If {@code n <= 0}, this returns the unparameterized {@link #VARBIT} codec.
   */
  public static Codec<io.codemine.java.postgresql.codecs.Bit> varbit(int n) {
    return new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.varbit(n));
  }

  /**
   * Returns a codec for PostgreSQL {@code varchar(n)} — a variable-length character string of at
   * most {@code n} characters.
   *
   * <p>If {@code n <= 0}, this returns the unparameterized {@link #VARCHAR} codec.
   */
  public static Codec<String> varchar(int n) {
    return new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.varchar(n));
  }

  /**
   * Returns a codec for PostgreSQL {@code bpchar(n)} — a fixed-length blank-padded character string
   * of exactly {@code n} characters.
   */
  public static Codec<String> bpchar(int n) {
    return new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.bpchar(n));
  }

  /**
   * Returns a codec for PostgreSQL enum types.
   *
   * @param schema the schema name. Empty string or null for the default schema (usually "public")
   * @param name the enum type name
   * @param valueToLabel a map of enum values to their corresponding labels
   * @param <A> the enum type
   * @return a {@link Codec} for the specified enum type
   */
  public static <A> Codec<A> enum_(String schema, String name, Map<A, String> valueToLabel) {
    return new AgnosticCodec<>(
        new io.codemine.java.postgresql.codecs.EnumCodec<>(schema, name, valueToLabel));
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
  public static <Z> Codec<Z> composite(
      String schema,
      String name,
      Function<Object[], Z> construct,
      io.codemine.java.postgresql.codecs.CompositeCodec.Field<Z, ?>... fields) {
    return new AgnosticCodec<>(
        new io.codemine.java.postgresql.codecs.CompositeCodec<>(schema, name, construct, fields));
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
  public static <Z, A> io.codemine.java.postgresql.codecs.CompositeCodec.Field<Z, A> field(
      String name, Codec<A> codec, Function<Z, A> getter) {
    return new io.codemine.java.postgresql.codecs.CompositeCodec.Field<>(
        name, getter, codec.toAgnostic());
  }

  /**
   * Converts this codec to an agnostic codec that can be used with any PostgreSQL client library.
   *
   * <p>This is required for composite codecs, which need to delegate to the underlying agnostic
   * codec for each field.
   */
  public io.codemine.java.postgresql.codecs.Codec<A> toAgnostic();

  /**
   * Binds a value to a prepared statement.
   *
   * @param ps the prepared statement
   * @param index the parameter index
   * @param value the value to bind
   * @throws SQLException if a database access error occurs
   */
  public void bind(PreparedStatement ps, int index, A value) throws SQLException;

  /**
   * Decodes a non-nullable value from the result set.
   *
   * @param rs the result set
   * @param row the row index
   * @param col the column index
   * @return the decoded value
   * @throws SQLException if a database access error occurs or the value is null
   */
  public A decodeNonNullable(ResultSet rs, int row, int col) throws SQLException;

  /**
   * Decodes a nullable value from the result set.
   *
   * @param rs the result set
   * @param row the row index
   * @param col the column index
   * @return the decoded value, or null if the value is SQL NULL
   * @throws SQLException if a database access error occurs
   */
  public A decodeNullable(ResultSet rs, int row, int col) throws SQLException;

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
  default Optional<A> decodeOptional(ResultSet rs, int row, int col) throws SQLException {
    return Optional.ofNullable(decodeNullable(rs, row, col));
  }
}
