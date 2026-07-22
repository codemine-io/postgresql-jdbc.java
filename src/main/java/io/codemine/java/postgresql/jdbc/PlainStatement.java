package io.codemine.java.postgresql.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation backing {@link Statement#plain(String)} and {@link Statement#plain(String,
 * boolean, String)}.
 */
final class PlainStatement implements Statement<Void> {

  private final Optional<String> name;
  private final boolean idempotent;
  private final String sql;

  PlainStatement(Optional<String> name, boolean idempotent, String sql) {
    this.name = name;
    this.idempotent = idempotent;
    this.sql = Objects.requireNonNull(sql, "sql");
  }

  @Override
  public String sql() {
    return sql;
  }

  @Override
  public void bindParams(PreparedStatement ps) {
    // No parameters to bind.
  }

  @Override
  public boolean returnsRows() {
    return false;
  }

  @Override
  public Void decodeResultSet(ResultSet rs) {
    throw new UnsupportedOperationException(
        "plain() statements do not return rows and should never have decodeResultSet called");
  }

  @Override
  public Void decodeAffectedRows(long affectedRows) {
    return null;
  }

  @Override
  public String statementName() {
    return name.orElseGet(Statement.super::statementName);
  }

  @Override
  public boolean idempotent() {
    return idempotent;
  }
}
