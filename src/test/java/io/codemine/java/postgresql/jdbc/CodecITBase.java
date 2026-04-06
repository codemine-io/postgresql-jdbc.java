package io.codemine.java.postgresql.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Shrinkable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for pgjdbc integration tests. Spins up a PostgreSQL testcontainer and exercises each
 * codec's {@link Codec#bind}, {@link Codec#decodeNullable}, {@link Codec#decodeNonNullable}, {@link
 * Codec#decodeOptional}, and array roundtrip behavior against a real database.
 *
 * <p>Subclasses only need to provide the codec under test via the constructor.
 *
 * @param <A> the Java type handled by the codec
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class CodecITBase<A> {

  static final PostgreSQLContainer<?> container;

  static {
    container =
        new PostgreSQLContainer<>("postgres:18").withCommand("postgres -c max_connections=300");
    container.start();
  }

  private static final ConcurrentHashMap<Class<?>, java.sql.Connection> connectionsByClass =
      new ConcurrentHashMap<>();

  private final Codec<A> codec;
  private final Codec<List<A>> arrayCodec;
  private final Codec<List<List<A>>> arrayArrayCodec;
  private final java.sql.Connection connection;

  protected CodecITBase(Codec<A> codec) {
    this.codec = codec;
    arrayCodec = codec.inDim();
    arrayArrayCodec = arrayCodec.inDim();
    connection = connectionsByClass.computeIfAbsent(this.getClass(), cls -> openConnection());
  }

  private static java.sql.Connection openConnection() {
    try {
      var props = new java.util.Properties();
      props.setProperty("user", container.getUsername());
      props.setProperty("password", container.getPassword());
      // Disable server-side prepared-statement caching so result columns remain in text format.
      props.setProperty("prepareThreshold", "0");
      return DriverManager.getConnection(container.getJdbcUrl(), props);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to open pgjdbc connection", e);
    }
  }

  @SuppressWarnings("unused")
  @AfterAll
  void closeConnection() throws Exception {
    var conn = connectionsByClass.remove(this.getClass());
    if (conn != null) {
      conn.close();
    }
  }

  // -------------------------------------------------------------------------
  // Value generator
  // -------------------------------------------------------------------------

  @SuppressWarnings("unused")
  @Provide
  Arbitrary<A> values() {
    return net.jqwik.api.Arbitraries.fromGeneratorWithSize(
        size -> r -> Shrinkable.unshrinkable(codec.toAgnostic().random(r, size)));
  }

  @SuppressWarnings("unused")
  @Provide
  Arbitrary<List<A>> arrayValues() {
    return net.jqwik.api.Arbitraries.fromGeneratorWithSize(
        size -> r -> Shrinkable.unshrinkable(arrayCodec.toAgnostic().random(r, size)));
  }

  @SuppressWarnings("unused")
  @Provide
  Arbitrary<List<List<A>>> arrayArrayValues() {
    return net.jqwik.api.Arbitraries.fromGeneratorWithSize(
        size -> r -> Shrinkable.unshrinkable(arrayArrayCodec.toAgnostic().random(r, size)));
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private A roundtrip(A value) throws SQLException {
    String typeSig = codec.toAgnostic().typeSig();
    try (var ps = connection.prepareStatement("SELECT ?::" + typeSig)) {
      codec.bind(ps, 1, value);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Expected a result row");
        return codec.decodeNullable(rs, 1, 1);
      }
    }
  }

  private List<A> roundtripArray(List<A> value) throws SQLException {
    String typeSig = arrayCodec.toAgnostic().typeSig();
    try (var ps = connection.prepareStatement("SELECT ?::" + typeSig)) {
      arrayCodec.bind(ps, 1, value);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Expected a result row");
        return arrayCodec.decodeNullable(rs, 1, 1);
      }
    }
  }

  private List<List<A>> roundtripArrayArray(List<List<A>> value) throws SQLException {
    String typeSig = arrayArrayCodec.toAgnostic().typeSig();
    try (var ps = connection.prepareStatement("SELECT ?::" + typeSig)) {
      arrayArrayCodec.bind(ps, 1, value);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Expected a result row");
        return arrayArrayCodec.decodeNullable(rs, 1, 1);
      }
    }
  }

  // -------------------------------------------------------------------------
  // Roundtrip tests
  // -------------------------------------------------------------------------

  /**
   * Property test: a non-null value bound and decoded via pgjdbc must equal the original. Exercises
   * the core encode -> transmit -> decode path against a live PostgreSQL instance.
   */
  @Property(tries = 100)
  void roundtripsNonNull(@ForAll("values") A value) throws Exception {
    A decoded = roundtrip(value);
    assertEquals(
        value,
        decoded,
        "Roundtrip mismatch for " + codec.toAgnostic().typeSig() + " value=" + value);
  }

  // -------------------------------------------------------------------------
  // Null-handling tests
  // -------------------------------------------------------------------------

  /**
   * Binding {@code null} and calling {@link Codec#decodeNullable} must return {@code null}.
   * Verifies that each codec correctly propagates SQL NULL through the JDBC layer.
   */
  @Test
  void nullDecodesAsNull() throws Exception {
    A decoded = roundtrip(null);
    assertNull(decoded, "Expected null for NULL bind of " + codec.toAgnostic().typeSig());
  }

  /**
   * Binding {@code null} and calling {@link Codec#decodeNonNullable} must throw {@link
   * SQLException}. Verifies the non-null safety guard.
   */
  @Test
  void decodeNonNullableThrowsOnNull() throws Exception {
    String typeSig = codec.toAgnostic().typeSig();
    try (var ps = connection.prepareStatement("SELECT ?::" + typeSig)) {
      codec.bind(ps, 1, null);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Expected a result row");
        SQLException thrown =
            assertThrows(SQLException.class, () -> codec.decodeNonNullable(rs, 1, 1));
        assertEquals("22004", thrown.getSQLState());
      }
    }
  }

  /**
   * Binding {@code null} and calling {@link Codec#decodeOptional} must return {@link
   * Optional#empty()}. Verifies the Optional-based null handling.
   */
  @Test
  void decodeOptionalEmptyForNull() throws Exception {
    String typeSig = codec.toAgnostic().typeSig();
    try (var ps = connection.prepareStatement("SELECT ?::" + typeSig)) {
      codec.bind(ps, 1, null);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Expected a result row");
        Optional<A> result = codec.decodeOptional(rs, 1, 1);
        assertTrue(result.isEmpty(), "Expected Optional.empty() for NULL of " + typeSig);
      }
    }
  }

  /**
   * Property test: binding a non-null value and decoding via {@link Codec#decodeOptional} must
   * return a present {@link Optional} wrapping the original value.
   */
  @Property(tries = 100)
  void decodeOptionalWrapsNonNull(@ForAll("values") A value) throws Exception {
    String typeSig = codec.toAgnostic().typeSig();
    try (var ps = connection.prepareStatement("SELECT ?::" + typeSig)) {
      codec.bind(ps, 1, value);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Expected a result row");
        Optional<A> result = codec.decodeOptional(rs, 1, 1);
        assertTrue(result.isPresent(), "Expected non-empty Optional for " + typeSig);
        assertEquals(value, result.get(), "Optional value mismatch for " + typeSig);
      }
    }
  }

  // -------------------------------------------------------------------------
  // Array roundtrip tests
  // -------------------------------------------------------------------------

  /**
   * Property test: a non-null array bound and decoded via pgjdbc must equal the original. Exercises
   * the same encode→transmit→decode path as the scalar test, but for {@code A[]}.
   */
  @Property(tries = 100)
  void roundtripsArrayNonNull(@ForAll("arrayValues") List<A> value) throws Exception {
    List<A> decoded = roundtripArray(value);
    assertEquals(
        value,
        decoded,
        "Roundtrip mismatch for " + arrayCodec.toAgnostic().typeSig() + " value=" + value);
  }

  /** Binding {@code null} and decoding the array as nullable must return {@code null}. */
  @Test
  void nullArrayDecodesAsNull() throws Exception {
    List<A> decoded = roundtripArray(null);
    assertNull(decoded, "Expected null for NULL bind of " + arrayCodec.toAgnostic().typeSig());
  }

  /**
   * Binding {@code null} and decoding the array as non-nullable must throw {@link SQLException}.
   */
  @Test
  void arrayDecodeNonNullableThrowsOnNull() throws Exception {
    String typeSig = arrayCodec.toAgnostic().typeSig();
    try (var ps = connection.prepareStatement("SELECT ?::" + typeSig)) {
      arrayCodec.bind(ps, 1, null);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Expected a result row");
        SQLException thrown =
            assertThrows(SQLException.class, () -> arrayCodec.decodeNonNullable(rs, 1, 1));
        assertEquals("22004", thrown.getSQLState());
      }
    }
  }

  /**
   * Binding {@code null} and decoding the array as optional must return {@link Optional#empty()}.
   */
  @Test
  void arrayDecodeOptionalEmptyForNull() throws Exception {
    String typeSig = arrayCodec.toAgnostic().typeSig();
    try (var ps = connection.prepareStatement("SELECT ?::" + typeSig)) {
      arrayCodec.bind(ps, 1, null);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Expected a result row");
        Optional<List<A>> result = arrayCodec.decodeOptional(rs, 1, 1);
        assertTrue(result.isEmpty(), "Expected Optional.empty() for NULL of " + typeSig);
      }
    }
  }

  /**
   * Property test: binding a non-null array and decoding via {@link Codec#decodeOptional} must
   * return a present {@link Optional} wrapping the original value.
   */
  @Property(tries = 100)
  void arrayDecodeOptionalWrapsNonNull(@ForAll("arrayValues") List<A> value) throws Exception {
    String typeSig = arrayCodec.toAgnostic().typeSig();
    try (var ps = connection.prepareStatement("SELECT ?::" + typeSig)) {
      arrayCodec.bind(ps, 1, value);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Expected a result row");
        Optional<List<A>> result = arrayCodec.decodeOptional(rs, 1, 1);
        assertTrue(result.isPresent(), "Expected non-empty Optional for " + typeSig);
        assertEquals(value, result.get(), "Optional value mismatch for " + typeSig);
      }
    }
  }

  // -------------------------------------------------------------------------
  // 2D array roundtrip tests
  // -------------------------------------------------------------------------

  /**
   * Property test: a non-null 2D array bound and decoded via pgjdbc must equal the original.
   * Exercises the same encode -> transmit -> decode path as the 1D array test, but for {@code
   * A[][]}.
   */
  @Property(tries = 100)
  void roundtripsArrayArrayNonNull(@ForAll("arrayArrayValues") List<List<A>> value)
      throws Exception {
    List<List<A>> decoded = roundtripArrayArray(value);
    assertEquals(
        value,
        decoded,
        "Roundtrip mismatch for " + arrayArrayCodec.toAgnostic().typeSig() + " value=" + value);
  }

  /** Binding {@code null} and decoding the 2D array as nullable must return {@code null}. */
  @Test
  void nullArrayArrayDecodesAsNull() throws Exception {
    List<List<A>> decoded = roundtripArrayArray(null);
    assertNull(decoded, "Expected null for NULL bind of " + arrayArrayCodec.toAgnostic().typeSig());
  }

  /**
   * Binding {@code null} and decoding the 2D array as non-nullable must throw {@link SQLException}.
   */
  @Test
  void arrayArrayDecodeNonNullableThrowsOnNull() throws Exception {
    String typeSig = arrayArrayCodec.toAgnostic().typeSig();
    try (var ps = connection.prepareStatement("SELECT ?::" + typeSig)) {
      arrayArrayCodec.bind(ps, 1, null);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Expected a result row");
        SQLException thrown =
            assertThrows(SQLException.class, () -> arrayArrayCodec.decodeNonNullable(rs, 1, 1));
        assertEquals("22004", thrown.getSQLState());
      }
    }
  }

  /**
   * Binding {@code null} and decoding the 2D array as optional must return {@link
   * Optional#empty()}.
   */
  @Test
  void arrayArrayDecodeOptionalEmptyForNull() throws Exception {
    String typeSig = arrayArrayCodec.toAgnostic().typeSig();
    try (var ps = connection.prepareStatement("SELECT ?::" + typeSig)) {
      arrayArrayCodec.bind(ps, 1, null);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Expected a result row");
        Optional<List<List<A>>> result = arrayArrayCodec.decodeOptional(rs, 1, 1);
        assertTrue(result.isEmpty(), "Expected Optional.empty() for NULL of " + typeSig);
      }
    }
  }

  /**
   * Property test: binding a non-null 2D array and decoding via {@link Codec#decodeOptional} must
   * return a present {@link Optional} wrapping the original value.
   */
  @Property(tries = 100)
  void arrayArrayDecodeOptionalWrapsNonNull(@ForAll("arrayArrayValues") List<List<A>> value)
      throws Exception {
    String typeSig = arrayArrayCodec.toAgnostic().typeSig();
    try (var ps = connection.prepareStatement("SELECT ?::" + typeSig)) {
      arrayArrayCodec.bind(ps, 1, value);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "Expected a result row");
        Optional<List<List<A>>> result = arrayArrayCodec.decodeOptional(rs, 1, 1);
        assertTrue(result.isPresent(), "Expected non-empty Optional for " + typeSig);
        assertEquals(value, result.get(), "Optional value mismatch for " + typeSig);
      }
    }
  }
}
