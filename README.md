# postgresql-jdbc

[![docs](https://img.shields.io/badge/docs-all-white)](https://codemine.io/postgresql-jdbc.java/)
[![maven](https://img.shields.io/badge/maven-latest-green)](https://codemine.io/postgresql-jdbc.java/latest/)
[![javadoc](https://img.shields.io/badge/javadoc-latest-green)](https://codemine.io/postgresql-jdbc.java/latest/apidocs/)
[![javadoc](https://javadoc.io/badge2/io.codemine.java.postgresql/jdbc/javadoc.svg)](https://javadoc.io/doc/io.codemine.java.postgresql/jdbc)
[![Maven Central Version](https://img.shields.io/maven-central/v/io.codemine.java.postgresql/jdbc)](https://central.sonatype.com/artifact/io.codemine.java.postgresql/jdbc)

JDBC PostgreSQL Driver Extensions — helpers and adapters that integrate
the `postgresql-codecs` library with the pgjdbc driver to provide
lossless, type-safe handling of many PostgreSQL types (arrays, ranges,
multiranges, composites, enums, JSON, network and geometric types, etc.).

## Motivation

The stock JDBC driver covers the basics but lacks convenient, type-safe
support for many PostgreSQL-specific types. This module contains
pgjdbc-specific adapters and utilities that make it easy to use the
`postgresql-codecs` codec implementations with `org.postgresql`.

## Features

- `Statement<R>` abstraction for packaging SQL, parameter binding and result decoding
- Driver adapter utilities for using `Codec<A>` with pgjdbc
- Helpers for encoding values as `PGobject` using codec text serialization
- Convenience support for arrays, composite types, enums and domains
- Jackson-based JSON/JSONB integration
- Well-tested against live PostgreSQL using Testcontainers

## Installation

The package is published to Maven Central under [`io.codemine.java.postgresql:jdbc`](https://central.sonatype.com/artifact/io.codemine.java.postgresql/jdbc).

This module depends on `postgresql-codecs` and the official `org.postgresql` driver.

## Usage with JDBC (pgjdbc)

The usual pattern is to wrap SQL, parameter binding and result decoding in
a `Statement<R>`. Its `execute(connection)` method prepares the statement,
binds parameters, and chooses `execute()` or `executeUpdate()` based on
`returnsRows()`.

```java
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.ArrayList;
import io.codemine.java.postgresql.jdbc.Codec;
import io.codemine.java.postgresql.jdbc.Statement;

public record SelectAlbumByName(String name)
        implements Statement<ArrayList<SelectAlbumByName.ResultRow>> {

    public record ResultRow(
            long id,
            String name,
            LocalDate released) {}
    
    public String sql() {
        return "select id, name, released from album where name = ?";
    }

    public void bindParams(PreparedStatement ps) throws SQLException {
        Codec.TEXT.bind(ps, 1, this.name());
    }

    public boolean returnsRows() {
        return true;
    }

    public ArrayList<ResultRow> decodeResultSet(ResultSet rs) throws SQLException {
        ArrayList<ResultRow> output = new ArrayList<>();
        int row = 0;
        
        while (rs.next()) {
            long idCol = Codec.INT8.decodeNonNullable(rs, row, 1);
            String nameCol = Codec.TEXT.decodeNonNullable(rs, row, 2);
            LocalDate releasedCol = Codec.DATE.decodeNullable(rs, row, 3);

            output.add(new ResultRow(idCol, nameCol, releasedCol));
            row++;
        }

        return output;
    }

    public ArrayList<ResultRow> decodeAffectedRows(long affectedRows) {
        throw new UnsupportedOperationException();
    }
}
```

Then you can execute the statement and get back a fully decoded result:

```java
List<SelectAlbumByName.ResultRow> result =
		new SelectAlbumByName("The Dark Side of the Moon")
				.execute(jdbcConnection);
```

## Batching

For repeated update statements with the same SQL, there is a utility class `StatementBatch<R>`. You can create a batch from an iterable using `new StatementBatch<>(statements)`, which prepares once, runs a JDBC batch, and returns each statement's decoded affected-row result in order. For convenience, there is also a varargs constructor as in the example below.

```java
StatementBatch<Long> batch = StatementBatch.of(
    new UpdateAlbumReleaseDate(1, LocalDate.of(1973, 3, 1)),
    new UpdateAlbumReleaseDate(2, LocalDate.of(1973, 11, 2))
);
List<Long> affectedRows = batch.execute(jdbcConnection);
```

The batch may be constructed once and executed multiple times with different connections, as needed.

## Compatibility Check (Startup / Liveness Probes)

Each `Statement` exposes a `checkCompatibility(Connection conn)` default method that validates the
statement against the current database schema without executing it for real. It is intended for use
in startup or liveness probes that need to detect schema drift.

**How it works**

1. Issues `EXPLAIN (VERBOSE, FORMAT JSON)` with the same bind parameters to force PostgreSQL to
   parse and plan the query against the current schema. If a table or column has been dropped or
   renamed the EXPLAIN will fail, surfacing the drift immediately.
2. Reads JDBC parameter and result metadata from the prepared statement and compares inferred
   PostgreSQL types against the codecs declared by `paramCodecs()` and `resultCodecs()`.
3. Returns a `StatementCompatibilityReport` with an overall `isCompatible()` flag, structured
   mismatch lists, and a `summary()` string suitable for log output or a Kubernetes liveness
   endpoint.

**Type matching is assignment-compatible.** Members of the text family (`text`, `varchar`, `bpchar`,
`citext`) are mutually compatible, as are members of the integer family (`int2`, `int4`, `int8`) and
the floating-point family (`float4`, `float8`, `numeric`). This avoids false positives for harmless
type widening or column-type aliases.

**Usage example**

Extend the `Statement` record from the example above to supply codec metadata:

```java
public record SelectAlbumByName(String name)
        implements Statement<ArrayList<SelectAlbumByName.ResultRow>> {

    public record ResultRow(long id, String name, LocalDate released) {}

    public String sql() {
        return "select id, name, released from album where name = ?";
    }

    public void bindParams(PreparedStatement ps) throws SQLException {
        Codec.TEXT.bind(ps, 1, this.name());
    }

    public boolean returnsRows() { return true; }

    // Codec metadata for the compatibility check
    public List<Codec<?>> paramCodecs() {
        return List.of(Codec.TEXT);
    }

    public List<Codec<?>> resultCodecs() {
        return List.of(Codec.INT8, Codec.TEXT, Codec.DATE);
    }

    public ArrayList<ResultRow> decodeResultSet(ResultSet rs) throws SQLException {
        // … decoding logic …
    }

    public ArrayList<ResultRow> decodeAffectedRows(long affectedRows) {
        throw new UnsupportedOperationException();
    }
}
```

Then check compatibility on startup or in a health endpoint:

```java
StatementCompatibilityReport report =
    new SelectAlbumByName("probe").checkCompatibility(jdbcConnection);

if (!report.isCompatible()) {
    log.error("Schema drift detected: {}", report.summary());
    // surface mismatches individually if needed
    for (var m : report.parameterMismatches()) {
        log.error("  param[{}] expected={} inferred={}: {}",
            m.index(), m.expectedType(), m.inferredType(), m.explanation());
    }
    for (var m : report.resultMismatches()) {
        log.error("  col[{}] expected={} inferred={}: {}",
            m.index(), m.expectedType(), m.inferredType(), m.explanation());
    }
}
```

The check is side-effect free: `EXPLAIN` plans but never executes the query, and the probe never
touches application data.

