package io.codemine.java.postgresql.jdbc;

import java.util.List;

/**
 * The result of a {@link Statement#checkCompatibility(java.sql.Connection) compatibility check}.
 * Captures whether the statement's declared codecs are consistent with the types PostgreSQL infers
 * for its parameters and result columns.
 *
 * <p>Type comparison uses assignment-compatible PostgreSQL type rules: for example, {@code text}
 * and {@code varchar} are considered compatible, as are members of the integer family.
 */
public final class StatementCompatibilityReport {

  /**
   * A mismatch between the codec type declared for a parameter and the type PostgreSQL inferred for
   * that parameter slot.
   */
  public record ParameterMismatch(
      int index, String expectedType, String inferredType, String explanation) {}

  /**
   * A mismatch between the codec type declared for a result column and the type PostgreSQL inferred
   * for that column.
   */
  public record ResultMismatch(
      int index, String expectedType, String inferredType, String explanation) {}

  private final boolean compatible;
  private final List<ParameterMismatch> parameterMismatches;
  private final List<ResultMismatch> resultMismatches;

  StatementCompatibilityReport(
      boolean compatible,
      List<ParameterMismatch> parameterMismatches,
      List<ResultMismatch> resultMismatches) {
    this.compatible = compatible;
    this.parameterMismatches = List.copyOf(parameterMismatches);
    this.resultMismatches = List.copyOf(resultMismatches);
  }

  /**
   * Returns {@code true} if no type mismatches were detected.
   *
   * @return {@code true} if compatible
   */
  public boolean isCompatible() {
    return compatible;
  }

  /**
   * Returns the list of parameter mismatches, ordered by parameter index.
   *
   * @return parameter mismatches
   */
  public List<ParameterMismatch> parameterMismatches() {
    return parameterMismatches;
  }

  /**
   * Returns the list of result-column mismatches, ordered by column index.
   *
   * @return result mismatches
   */
  public List<ResultMismatch> resultMismatches() {
    return resultMismatches;
  }

  /**
   * Returns a compact human-readable summary suitable for logging or Kubernetes liveness probes.
   *
   * @return a summary string
   */
  public String summary() {
    if (compatible) {
      return "compatible";
    }
    var sb = new StringBuilder("incompatible");
    if (!parameterMismatches.isEmpty()) {
      sb.append("; parameters:");
      for (var m : parameterMismatches) {
        sb.append(" [")
            .append(m.index())
            .append("] expected=")
            .append(m.expectedType())
            .append(" inferred=")
            .append(m.inferredType());
      }
    }
    if (!resultMismatches.isEmpty()) {
      sb.append("; results:");
      for (var m : resultMismatches) {
        sb.append(" [")
            .append(m.index())
            .append("] expected=")
            .append(m.expectedType())
            .append(" inferred=")
            .append(m.inferredType());
      }
    }
    return sb.toString();
  }
}
