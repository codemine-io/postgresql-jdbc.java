package io.codemine.java.postgresql.jdbc;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * A codec for PostgreSQL types intended for use with pgJDBC. This interface provides methods for
 * binding values to prepared statements and decoding values from result sets.
 *
 * @param <A> the Java type handled by this codec
 */
public interface Codec<A> {

  /** Codec for SQL int4 (integer). */
  static final Codec<Integer> INT4 = new Int4Codec();

  /** Codec for SQL text. */
  static final Codec<String> TEXT = new TextCodec();

  /** Codec for SQL inet (IP address). */
  static final Codec<io.codemine.java.postgresql.codecs.Inet> INET =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.INET);

  /** Codec for SQL macaddr. */
  static final Codec<io.codemine.java.postgresql.codecs.Macaddr> MACADDR =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.MACADDR);

  /** Codec for SQL boolean. */
  static final Codec<Boolean> BOOL = new BoolCodec();

  /** Codec for SQL int2 (smallint). */
  static final Codec<Short> INT2 = new Int2Codec();

  /** Codec for SQL int8 (bigint). */
  static final Codec<Long> INT8 = new Int8Codec();

  /** Codec for SQL float4 (real). */
  static final Codec<Float> FLOAT4 = new Float4Codec();

  /** Codec for SQL float8 (double). */
  static final Codec<Double> FLOAT8 = new Float8Codec();

  /** Codec for SQL numeric/decimal. */
  static final Codec<java.math.BigDecimal> NUMERIC = new NumericCodec();

  /** Codec for SQL bytea (binary). */
  static final Codec<io.codemine.java.postgresql.codecs.Bytea> BYTEA = new ByteaCodec();

  /** Codec for SQL uuid. */
  static final Codec<java.util.UUID> UUID = new UuidCodec();

  /** Codec for SQL json (text). */
  static final Codec<JsonNode> JSON =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.JSON);

  /** Codec for SQL jsonb (binary JSON). */
  static final Codec<JsonNode> JSONB =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.JSONB);

  /** Codec for SQL varchar. */
  static final Codec<String> VARCHAR = new VarcharCodec();

  /** Codec for SQL bpchar (char(n)). */
  static final Codec<String> BPCHAR = new BpcharCodec();

  /** Codec for SQL "char" (internal type). */
  static final Codec<Byte> CHAR =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.CHAR);

  /** Codec for PostgreSQL OID. */
  static final Codec<Integer> OID = new OidCodec();

  /** Codec for SQL money. */
  static final Codec<Long> MONEY =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.MONEY);

  /** Codec for SQL date. */
  static final Codec<java.time.LocalDate> DATE = new DateCodec();

  /** Codec for SQL time. */
  static final Codec<java.time.LocalTime> TIME = new TimeCodec();

  /** Codec for SQL timetz. */
  static final Codec<io.codemine.java.postgresql.codecs.Timetz> TIMETZ =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.TIMETZ);

  /** Codec for SQL timestamp without timezone. */
  static final Codec<java.time.LocalDateTime> TIMESTAMP = new TimestampCodec();

  /** Codec for SQL timestamptz (timestamp with timezone). */
  static final Codec<java.time.Instant> TIMESTAMPTZ = new TimestamptzCodec();

  /** Codec for SQL interval. */
  static final Codec<io.codemine.java.postgresql.codecs.Interval> INTERVAL =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.INTERVAL);

  /** Codec for SQL point. */
  static final Codec<io.codemine.java.postgresql.codecs.Point> POINT =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.POINT);

  /** Codec for SQL line. */
  static final Codec<io.codemine.java.postgresql.codecs.Line> LINE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.LINE);

  /** Codec for SQL lseg. */
  static final Codec<io.codemine.java.postgresql.codecs.Lseg> LSEG =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.LSEG);

  /** Codec for SQL box. */
  static final Codec<io.codemine.java.postgresql.codecs.Box> BOX =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.BOX);

  /** Codec for SQL path. */
  static final Codec<io.codemine.java.postgresql.codecs.Path> PATH =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.PATH);

  /** Codec for SQL polygon. */
  static final Codec<io.codemine.java.postgresql.codecs.Polygon> POLYGON =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.POLYGON);

  /** Codec for SQL circle. */
  static final Codec<io.codemine.java.postgresql.codecs.Circle> CIRCLE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.CIRCLE);

  /** Codec for SQL cidr. */
  static final Codec<io.codemine.java.postgresql.codecs.Cidr> CIDR =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.CIDR);

  /** Codec for SQL macaddr8. */
  static final Codec<io.codemine.java.postgresql.codecs.Macaddr8> MACADDR8 =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.MACADDR8);

  /** Codec for SQL bit. */
  static final Codec<io.codemine.java.postgresql.codecs.Bit> BIT =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.BIT);

  /** Codec for SQL varbit. */
  static final Codec<io.codemine.java.postgresql.codecs.Bit> VARBIT =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.VARBIT);

  /** Codec for SQL citext. */
  static final Codec<String> CITEXT = new CitextCodec();

  /** Codec for SQL tsvector. */
  static final Codec<io.codemine.java.postgresql.codecs.Tsvector> TSVECTOR =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.TSVECTOR);

  /** Codec for SQL hstore. */
  static final Codec<io.codemine.java.postgresql.codecs.Hstore> HSTORE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.HSTORE);

  /** Codec for SQL int4range. */
  static final Codec<io.codemine.java.postgresql.codecs.Range<Integer>> INT4RANGE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.INT4RANGE);

  /** Codec for SQL int8range. */
  static final Codec<io.codemine.java.postgresql.codecs.Range<Long>> INT8RANGE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.INT8RANGE);

  /** Codec for SQL numrange. */
  static final Codec<io.codemine.java.postgresql.codecs.Range<java.math.BigDecimal>> NUMRANGE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.NUMRANGE);

  /** Codec for SQL tsrange. */
  static final Codec<io.codemine.java.postgresql.codecs.Range<java.time.LocalDateTime>> TSRANGE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.TSRANGE);

  /** Codec for SQL tstzrange. */
  static final Codec<io.codemine.java.postgresql.codecs.Range<java.time.Instant>> TSTZRANGE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.TSTZRANGE);

  /** Codec for SQL daterange. */
  static final Codec<io.codemine.java.postgresql.codecs.Range<java.time.LocalDate>> DATERANGE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.DATERANGE);

  /** Codec for SQL int4multirange. */
  static final Codec<io.codemine.java.postgresql.codecs.Multirange<Integer>> INT4MULTIRANGE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.INT4MULTIRANGE);

  /** Codec for SQL int8multirange. */
  static final Codec<io.codemine.java.postgresql.codecs.Multirange<Long>> INT8MULTIRANGE =
      new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.INT8MULTIRANGE);

  /** Codec for SQL nummultirange. */
  static final Codec<io.codemine.java.postgresql.codecs.Multirange<java.math.BigDecimal>>
      NUMMULTIRANGE = new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.NUMMULTIRANGE);

  /** Codec for SQL tsmultirange. */
  static final Codec<io.codemine.java.postgresql.codecs.Multirange<java.time.LocalDateTime>>
      TSMULTIRANGE = new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.TSMULTIRANGE);

  /** Codec for SQL tstzmultirange. */
  static final Codec<io.codemine.java.postgresql.codecs.Multirange<java.time.Instant>>
      TSTZMULTIRANGE = new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.TSTZMULTIRANGE);

  /** Codec for SQL datemultirange. */
  static final Codec<io.codemine.java.postgresql.codecs.Multirange<java.time.LocalDate>>
      DATEMULTIRANGE = new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.DATEMULTIRANGE);

  /**
   * Returns a codec for PostgreSQL {@code bit(n)} — a fixed-length bit string of exactly {@code n}
   * bits.
   *
   * <p>If {@code n <= 0}, this returns the unparameterized {@link #BIT} codec.
   *
   * @param n the number of bits
   * @return a codec for {@code bit(n)}
   */
  static Codec<io.codemine.java.postgresql.codecs.Bit> bit(int n) {
    return new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.bit(n));
  }

  /**
   * Returns a codec for PostgreSQL {@code varbit(n)} — a variable-length bit string of at most
   * {@code n} bits.
   *
   * <p>If {@code n <= 0}, this returns the unparameterized {@link #VARBIT} codec.
   *
   * @param n the maximum number of bits
   * @return a codec for {@code varbit(n)}
   */
  static Codec<io.codemine.java.postgresql.codecs.Bit> varbit(int n) {
    return new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.varbit(n));
  }

  /**
   * Returns a codec for PostgreSQL {@code varchar(n)} — a variable-length character string of at
   * most {@code n} characters.
   *
   * <p>If {@code n <= 0}, this returns the unparameterized {@link #VARCHAR} codec.
   *
   * @param n the maximum length in characters
   * @return a codec for {@code varchar(n)}
   */
  static Codec<String> varchar(int n) {
    return new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.varchar(n));
  }

  /**
   * Returns a codec for PostgreSQL {@code bpchar(n)} — a fixed-length blank-padded character string
   * of exactly {@code n} characters.
   *
   * @param n the fixed length in characters
   * @return a codec for {@code bpchar(n)}
   */
  static Codec<String> bpchar(int n) {
    return new AgnosticCodec<>(io.codemine.java.postgresql.codecs.Codec.bpchar(n));
  }

  /**
   * Returns a codec for PostgreSQL enum types.
   *
   * @param schema the schema name. Empty string or null for the default schema (usually ")
   * @param name the enum type name
   * @param valueToLabel a map of enum values to their corresponding labels
   * @param <A> the enum type
   * @return a {@link Codec} for the specified enum type
   */
  static <A> Codec<A> enumeration(String schema, String name, Map<A, String> valueToLabel) {
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
   * @param <Z> the composite type produced by the construct function
   * @return a {@link Codec} for the composite type
   */
  @SafeVarargs
  static <Z> Codec<Z> composite(
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
  static <Z, A> io.codemine.java.postgresql.codecs.CompositeCodec.Field<Z, A> field(
      String name, Codec<A> codec, Function<Z, A> getter) {
    return new io.codemine.java.postgresql.codecs.CompositeCodec.Field<>(
        name, getter, codec.toAgnostic());
  }

  /**
   * Converts this codec to an agnostic codec that can be used with any PostgreSQL client library.
   *
   * <p>This is required for composite codecs, which need to delegate to the underlying agnostic
   * codec for each field.
   *
   * @return an agnostic codec equivalent to this codec
   */
  io.codemine.java.postgresql.codecs.Codec<A> toAgnostic();

  /**
   * Binds a value to a prepared statement.
   *
   * @param ps the prepared statement
   * @param index the parameter index
   * @param value the value to bind
   * @throws SQLException if a database access error occurs
   */
  void bind(PreparedStatement ps, int index, A value) throws SQLException;

  /**
   * Decodes a nullable value from the result set.
   *
   * @param rs the result set
   * @param row the row index
   * @param col the column index
   * @return the decoded value, or null if the value is SQL NULL
   * @throws SQLException if a database access error occurs
   */
  A decodeNullable(ResultSet rs, int row, int col) throws SQLException;

  /**
   * Decodes a non-nullable value from the result set.
   *
   * @param rs the result set
   * @param row the row index
   * @param col the column index
   * @return the decoded value
   * @throws SQLException if a database access error occurs or the value is null
   */
  default A decodeNonNullable(ResultSet rs, int row, int col) throws SQLException {
    A value = decodeNullable(rs, row, col);
    if (value == null) {
      throw new SQLException("Unexpected NULL value at row " + row + ", column " + col, "22004");
    }
    return value;
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
  default Optional<A> decodeOptional(ResultSet rs, int row, int col) throws SQLException {
    return Optional.ofNullable(decodeNullable(rs, row, col));
  }

  /**
   * Add one dimension of array nesting to this codec, returning a codec for the corresponding
   * PostgreSQL array type. For example, if this is a codec for type {@code A}, the returned codec
   * will be for type {@code A[]}. This can be called repeatedly to create codecs for
   * multi-dimensional arrays, e.g. {@code A[][]}.
   *
   * @return a codec for a one-dimension higher array of this codec's type
   */
  default Codec<List<A>> inDim() {
    return new AgnosticCodec<>(this.toAgnostic().inDim());
  }
}
