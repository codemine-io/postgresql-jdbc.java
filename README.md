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

## Transactions

For a unit of work that must run atomically, there is `Transaction<R>`. Like `Statement`, you implement a single method — `run(connection)`, which may call `Statement.execute(connection)` any number of times — and get `execute(connection)` for free. It disables autocommit, runs your code, commits on success, rolls back on any exception, and restores the connection's original autocommit state.

```java
Transaction<Void> transferFunds = connection -> {
    new DebitAccount(fromId, amount).execute(connection);
    new CreditAccount(toId, amount).execute(connection);
    return null;
};

transferFunds.execute(jdbcConnection);
```

`Transaction.of(statement)` adapts a `Statement<R>` directly, and `andThen`/`map` compose transactions:

```java
Transaction<Void> transaction = Transaction
        .of(new DebitAccount(fromId, amount))
        .andThen(Transaction.of(new CreditAccount(toId, amount)));

transaction.execute(jdbcConnection);
```

### Settings: isolation level, read-only, retries

`execute(connection, settings)` takes a `TransactionSettings` for cases that need an isolation level, a read-only transaction, or automatic retries on a retryable failure (PostgreSQL's `serialization_failure` and `deadlock_detected` SQLSTATEs, plus `unique_violation`, since PostgreSQL may report a genuine serialization conflict under `SERIALIZABLE` isolation as a unique-constraint violation instead):

```java
TransactionSettings settings = TransactionSettings.DEFAULT
        .withIsolationLevel(IsolationLevel.SERIALIZABLE)
        .withMaxAttempts(5);

transferFunds.execute(jdbcConnection, settings);
```

A retry re-runs the whole `run(connection)` body from scratch, so it is only safe when that body has no side effects beyond the database itself. It's also not a substitute for `INSERT ... ON CONFLICT` when the transaction's own intent is an upsert: retrying a genuinely duplicate insert just reproduces the same `unique_violation` until attempts run out.

### Savepoints

`withSavepoint(onFailure)` wraps a transaction so that a `SQLException` rolls back to a savepoint — not the whole transaction — and `onFailure` supplies a fallback result instead:

```java
Transaction<Void> outer = connection -> {
    new InsertOrder(orderId).execute(connection);
    Transaction.of(new DecrementInventory(items))
            .withSavepoint((conn, e) -> null) // inventory step failed; order still commits
            .run(connection);
    return null;
};

outer.execute(jdbcConnection);
```
