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

This is the middle layer of a three-layer stack:
[`postgresql-codecs`](https://github.com/nikita-volkov/postgresql-codecs) (driver-agnostic types)
→ `postgresql-jdbc` (this library) →
[`rich-pg`](https://github.com/codemine-io/rich-pg.java) (the pooled, resilient runtime).
This library is the **JDBC contract layer**: `Statement<R>` and its JDBC codec
adapters describe how one statement talks to a raw `java.sql.Connection` —
what generated mapping code is written against, and what any runtime executes.
It is deliberately stateless: no connection pooling, retry policy, or
telemetry. `rich-pg` is the (currently sole) production runtime that fulfills
these contracts; other runtimes could implement them too.

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
