package io.codemine.java.postgresql.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

/** Integration tests for {@link Statement#checkCompatibility}. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StatementCompatibilityIT {

  private static final String TABLE = "compat_check_it";

  static final PostgreSQLContainer<?> container;
  static final HikariDataSource jdbcPool;

  static {
    container =
        new PostgreSQLContainer<>("postgres:18").withCommand("postgres -c max_connections=300");
    container.start();

    var hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(container.getJdbcUrl());
    hikariConfig.setUsername(container.getUsername());
    hikariConfig.setPassword(container.getPassword());
    hikariConfig.addDataSourceProperty("prepareThreshold", "0");
    hikariConfig.setMaximumPoolSize(10);
    jdbcPool = new HikariDataSource(hikariConfig);

    Runtime.getRuntime().addShutdownHook(new Thread(jdbcPool::close));
  }

  @BeforeEach
  void resetTable() throws SQLException {
    try (var conn = jdbcPool.getConnection();
        var s = conn.createStatement()) {
      s.executeUpdate("DROP TABLE IF EXISTS " + TABLE);
      s.executeUpdate(
          "CREATE TABLE "
              + TABLE
              + " (id INTEGER PRIMARY KEY, name TEXT NOT NULL, label VARCHAR(100))");
      s.executeUpdate("INSERT INTO " + TABLE + " VALUES (1, 'one', 'alpha')");
    }
  }

  // -------------------------------------------------------------------------
  // Compatible statement
  // -------------------------------------------------------------------------

  @Test
  void compatibleStatementReportsCompatible() throws Exception {
    var stmt = new SelectByIdCompatible(1);
    try (var conn = jdbcPool.getConnection()) {
      var report = stmt.checkCompatibility(conn);
      assertTrue(report.isCompatible(), "Expected compatible but got: " + report.summary());
      assertTrue(report.parameterMismatches().isEmpty());
      assertTrue(report.resultMismatches().isEmpty());
      assertEquals("compatible", report.summary());
    }
  }

  // -------------------------------------------------------------------------
  // Parameter type mismatch
  // -------------------------------------------------------------------------

  @Test
  void parameterMismatchIsReported() throws Exception {
    var stmt = new SelectByIdParamMismatch(1);
    try (var conn = jdbcPool.getConnection()) {
      var report = stmt.checkCompatibility(conn);
      assertFalse(report.isCompatible(), "Expected incompatible but got: " + report.summary());
      assertEquals(1, report.parameterMismatches().size());
      assertEquals(0, report.resultMismatches().size());

      var mismatch = report.parameterMismatches().get(0);
      assertEquals(1, mismatch.index());
      assertEquals("text", mismatch.expectedType());
      // PostgreSQL infers the parameter type as int4 from the WHERE id = ? context
      assertEquals("int4", mismatch.inferredType());
      assertTrue(report.summary().contains("incompatible"));
      assertTrue(report.summary().contains("parameters:"));
    }
  }

  // -------------------------------------------------------------------------
  // Result column type mismatch
  // -------------------------------------------------------------------------

  @Test
  void resultMismatchIsReported() throws Exception {
    var stmt = new SelectByIdResultMismatch(1);
    try (var conn = jdbcPool.getConnection()) {
      var report = stmt.checkCompatibility(conn);
      assertFalse(report.isCompatible(), "Expected incompatible but got: " + report.summary());
      assertEquals(0, report.parameterMismatches().size());
      assertEquals(1, report.resultMismatches().size());

      var mismatch = report.resultMismatches().get(0);
      assertEquals(2, mismatch.index());
      assertEquals("int4", mismatch.expectedType());
      // PostgreSQL reports the name column as text
      assertEquals("text", mismatch.inferredType());
      assertTrue(report.summary().contains("incompatible"));
      assertTrue(report.summary().contains("results:"));
    }
  }

  // -------------------------------------------------------------------------
  // Assignment-compatible: text codec vs varchar column
  // -------------------------------------------------------------------------

  @Test
  void textCodecWithVarcharColumnIsCompatible() throws Exception {
    var stmt = new SelectLabelTextCodec(1);
    try (var conn = jdbcPool.getConnection()) {
      var report = stmt.checkCompatibility(conn);
      assertTrue(
          report.isCompatible(),
          "text codec vs varchar column should be assignment-compatible: " + report.summary());
      assertTrue(report.parameterMismatches().isEmpty());
      assertTrue(report.resultMismatches().isEmpty());
    }
  }

  // -------------------------------------------------------------------------
  // Statement fixtures
  // -------------------------------------------------------------------------

  /** Compatible statement: param is INT4 for an integer PK, results match TEXT column. */
  private record SelectByIdCompatible(int id) implements Statement<List<SelectByIdCompatible.Row>> {

    record Row(int id, String name) {}

    @Override
    public String sql() {
      return "SELECT id, name FROM " + TABLE + " WHERE id = ?";
    }

    @Override
    public void bindParams(PreparedStatement ps) throws SQLException {
      ps.setInt(1, id);
    }

    @Override
    public boolean returnsRows() {
      return true;
    }

    @Override
    public List<Codec<?>> paramCodecs() {
      return List.of(Codec.INT4);
    }

    @Override
    public List<Codec<?>> resultCodecs() {
      return List.of(Codec.INT4, Codec.TEXT);
    }

    @Override
    public List<Row> decodeResultSet(ResultSet rs) throws SQLException {
      var result = new ArrayList<Row>();
      while (rs.next()) {
        result.add(new Row(rs.getInt(1), rs.getString(2)));
      }
      return result;
    }

    @Override
    public List<Row> decodeAffectedRows(long affectedRows) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Mismatched parameter: paramCodecs declares TEXT but the id column is INTEGER. bindParams still
   * binds the correct int value so EXPLAIN succeeds; the mismatch is in the codec declaration.
   */
  private record SelectByIdParamMismatch(int id)
      implements Statement<List<SelectByIdParamMismatch.Row>> {

    record Row(int id, String name) {}

    @Override
    public String sql() {
      return "SELECT id, name FROM " + TABLE + " WHERE id = ?";
    }

    @Override
    public void bindParams(PreparedStatement ps) throws SQLException {
      ps.setInt(1, id);
    }

    @Override
    public boolean returnsRows() {
      return true;
    }

    @Override
    public List<Codec<?>> paramCodecs() {
      // Intentionally wrong: declares TEXT but the column is INTEGER
      return List.of(Codec.TEXT);
    }

    @Override
    public List<Codec<?>> resultCodecs() {
      return List.of(Codec.INT4, Codec.TEXT);
    }

    @Override
    public List<Row> decodeResultSet(ResultSet rs) throws SQLException {
      var result = new ArrayList<Row>();
      while (rs.next()) {
        result.add(new Row(rs.getInt(1), rs.getString(2)));
      }
      return result;
    }

    @Override
    public List<Row> decodeAffectedRows(long affectedRows) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Mismatched result: resultCodecs declares INT4 for the second column but it is TEXT. Parameter
   * codec is correct so only a result mismatch is reported.
   */
  private record SelectByIdResultMismatch(int id)
      implements Statement<List<SelectByIdResultMismatch.Row>> {

    record Row(int id, String name) {}

    @Override
    public String sql() {
      return "SELECT id, name FROM " + TABLE + " WHERE id = ?";
    }

    @Override
    public void bindParams(PreparedStatement ps) throws SQLException {
      ps.setInt(1, id);
    }

    @Override
    public boolean returnsRows() {
      return true;
    }

    @Override
    public List<Codec<?>> paramCodecs() {
      return List.of(Codec.INT4);
    }

    @Override
    public List<Codec<?>> resultCodecs() {
      // Intentionally wrong: declares INT4 for name column which is TEXT
      return List.of(Codec.INT4, Codec.INT4);
    }

    @Override
    public List<Row> decodeResultSet(ResultSet rs) throws SQLException {
      var result = new ArrayList<Row>();
      while (rs.next()) {
        result.add(new Row(rs.getInt(1), rs.getString(2)));
      }
      return result;
    }

    @Override
    public List<Row> decodeAffectedRows(long affectedRows) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Assignment-compatible: resultCodecs declares TEXT but the label column is VARCHAR(100). Under
   * PostgreSQL assignment-compatible rules both belong to the text family, so this should pass.
   */
  private record SelectLabelTextCodec(int id) implements Statement<List<SelectLabelTextCodec.Row>> {

    record Row(String label) {}

    @Override
    public String sql() {
      return "SELECT label FROM " + TABLE + " WHERE id = ?";
    }

    @Override
    public void bindParams(PreparedStatement ps) throws SQLException {
      ps.setInt(1, id);
    }

    @Override
    public boolean returnsRows() {
      return true;
    }

    @Override
    public List<Codec<?>> paramCodecs() {
      return List.of(Codec.INT4);
    }

    @Override
    public List<Codec<?>> resultCodecs() {
      // TEXT codec declared for a VARCHAR(100) column — assignment-compatible
      return List.of(Codec.TEXT);
    }

    @Override
    public List<Row> decodeResultSet(ResultSet rs) throws SQLException {
      var result = new ArrayList<Row>();
      while (rs.next()) {
        result.add(new Row(rs.getString(1)));
      }
      return result;
    }

    @Override
    public List<Row> decodeAffectedRows(long affectedRows) {
      throw new UnsupportedOperationException();
    }
  }
}
