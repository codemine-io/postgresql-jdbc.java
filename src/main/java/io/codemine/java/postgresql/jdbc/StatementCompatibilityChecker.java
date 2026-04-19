package io.codemine.java.postgresql.jdbc;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Performs a side-effect-free compatibility check for a {@link Statement} against the current
 * database schema.
 *
 * <p>The check issues {@code EXPLAIN (VERBOSE, FORMAT JSON)} against the statement SQL with the
 * same bind parameters to force parse and plan validation against the current schema. It then uses
 * JDBC parameter and result metadata to compare inferred PostgreSQL types against the codec types
 * declared by {@link Statement#paramCodecs()} and {@link Statement#resultCodecs()}. Type comparison
 * follows assignment-compatible PostgreSQL type rules rather than strict equality.
 */
final class StatementCompatibilityChecker {

  private static final Set<String> TEXT_FAMILY =
      Set.of("text", "varchar", "bpchar", "citext", "character varying", "character", "name");

  private static final Set<String> INT_FAMILY =
      Set.of("int2", "int4", "int8", "smallint", "integer", "bigint");

  private static final Set<String> FLOAT_FAMILY =
      Set.of("float4", "float8", "real", "double precision", "numeric", "decimal");

  private StatementCompatibilityChecker() {}

  /**
   * Runs the compatibility check and returns a report.
   *
   * @param statement the statement to check
   * @param conn the JDBC connection to use for the check
   * @param <R> the statement result type
   * @return a {@link StatementCompatibilityReport} describing any type mismatches
   * @throws SQLException if a database access error occurs during the check
   */
  static <R> StatementCompatibilityReport check(Statement<R> statement, Connection conn)
      throws SQLException {
    // Run EXPLAIN to force parse and plan validation against the current schema.
    // Bind the statement's actual parameter values so the planner uses correct type context.
    String explainSql = "EXPLAIN (VERBOSE, FORMAT JSON) " + statement.sql();
    try (PreparedStatement explainPs = conn.prepareStatement(explainSql)) {
      statement.bindParams(explainPs);
      try (ResultSet ignored = explainPs.executeQuery()) {
        while (ignored.next()) {
          // consume to ensure EXPLAIN executes fully
        }
      }
    }

    List<StatementCompatibilityReport.ParameterMismatch> paramMismatches = new ArrayList<>();
    List<StatementCompatibilityReport.ResultMismatch> resultMismatches = new ArrayList<>();

    try (PreparedStatement ps = conn.prepareStatement(statement.sql())) {
      List<Codec<?>> paramCodecs = statement.paramCodecs();
      if (!paramCodecs.isEmpty()) {
        ParameterMetaData pmd = ps.getParameterMetaData();
        int paramCount = pmd.getParameterCount();
        for (int i = 0; i < paramCodecs.size() && i < paramCount; i++) {
          int jdbcIdx = i + 1;
          String expectedType = paramCodecs.get(i).toAgnostic().typeSig();
          String inferredType = pmd.getParameterTypeName(jdbcIdx);
          if (!isUnknown(inferredType) && !assignmentCompatible(expectedType, inferredType)) {
            paramMismatches.add(
                new StatementCompatibilityReport.ParameterMismatch(
                    jdbcIdx,
                    expectedType,
                    inferredType,
                    "codec type "
                        + expectedType
                        + " is not assignment-compatible with inferred type "
                        + inferredType));
          }
        }
      }

      List<Codec<?>> resultCodecs = statement.resultCodecs();
      if (!resultCodecs.isEmpty() && statement.returnsRows()) {
        ResultSetMetaData rmd = ps.getMetaData();
        if (rmd != null) {
          int colCount = rmd.getColumnCount();
          for (int i = 0; i < resultCodecs.size() && i < colCount; i++) {
            int jdbcIdx = i + 1;
            String expectedType = resultCodecs.get(i).toAgnostic().typeSig();
            String inferredType = rmd.getColumnTypeName(jdbcIdx);
            if (!isUnknown(inferredType) && !assignmentCompatible(expectedType, inferredType)) {
              resultMismatches.add(
                  new StatementCompatibilityReport.ResultMismatch(
                      jdbcIdx,
                      expectedType,
                      inferredType,
                      "codec type "
                          + expectedType
                          + " is not assignment-compatible with inferred type "
                          + inferredType));
            }
          }
        }
      }
    }

    boolean compatible = paramMismatches.isEmpty() && resultMismatches.isEmpty();
    return new StatementCompatibilityReport(compatible, paramMismatches, resultMismatches);
  }

  private static boolean isUnknown(String type) {
    return type == null || type.isEmpty() || "unknown".equalsIgnoreCase(type);
  }

  /**
   * Returns {@code true} when {@code expectedTypeSig} and {@code inferredJdbcType} are
   * assignment-compatible according to PostgreSQL type rules.
   *
   * <p>Strict equality is always compatible. Additionally, all members of the text family ({@code
   * text}, {@code varchar}, {@code bpchar}, {@code citext}) are mutually compatible, as are all
   * members of the integer and floating-point families.
   */
  static boolean assignmentCompatible(String expectedTypeSig, String inferredJdbcType) {
    String expected = canonicalize(expectedTypeSig);
    String inferred = canonicalize(inferredJdbcType);
    if (expected.equals(inferred)) {
      return true;
    }
    if (TEXT_FAMILY.contains(expected) && TEXT_FAMILY.contains(inferred)) {
      return true;
    }
    if (INT_FAMILY.contains(expected) && INT_FAMILY.contains(inferred)) {
      return true;
    }
    if (FLOAT_FAMILY.contains(expected) && FLOAT_FAMILY.contains(inferred)) {
      return true;
    }
    return false;
  }

  /**
   * Normalizes a PostgreSQL type name to a canonical lower-case form, converting
   * underscore-prefixed array notation ({@code _type}) to SQL bracket notation ({@code type[]}),
   * and mapping common aliases to their canonical names.
   */
  static String canonicalize(String type) {
    if (type == null) {
      return "";
    }
    type = type.toLowerCase().trim();
    // Convert JDBC array notation "_type" to SQL notation "type[]"
    int dims = 0;
    while (type.startsWith("_")) {
      dims++;
      type = type.substring(1);
    }
    // Also strip trailing "[]" that may already be in typeSig form
    while (type.endsWith("[]")) {
      dims++;
      type = type.substring(0, type.length() - 2);
    }
    // Map common aliases to canonical names
    type =
        switch (type) {
          case "character varying" -> "varchar";
          case "double precision" -> "float8";
          case "real" -> "float4";
          case "integer" -> "int4";
          case "smallint" -> "int2";
          case "bigint" -> "int8";
          case "boolean" -> "bool";
          case "character" -> "bpchar";
          case "decimal" -> "numeric";
          default -> type;
        };
    var sb = new StringBuilder(type);
    for (int i = 0; i < dims; i++) {
      sb.append("[]");
    }
    return sb.toString();
  }
}
