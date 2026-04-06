# postgresql-jdbc

[![docs](https://img.shields.io/badge/docs-all-white)](https://codemine.io/postgresql-jdbc.java/)
[![maven](https://img.shields.io/badge/maven-latest-green)](https://codemine.io/postgresql-jdbc.java/latest/)
[![javadoc](https://img.shields.io/badge/javadoc-latest-green)](https://codemine.io/postgresql-jdbc.java/latest/apidocs/)
[![javadoc](https://javadoc.io/badge2/io.codemine.java.postgresql/jdbc/javadoc.svg)](https://javadoc.io/doc/io.codemine.java.postgresql/jdbc)

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

- Driver adapter utilities for using `Codec<A>` with pgjdbc
- Helpers for encoding values as `PGobject` using codec text serialization
- Convenience support for arrays, composite types, enums and domains
- Jackson-based JSON/JSONB integration
- Well-tested against live PostgreSQL using Testcontainers

## Installation

The package is published to Maven Central under [`io.codemine.java.postgresql:jdbc`](https://central.sonatype.com/artifact/io.codemine.java.postgresql/jdbc).

```xml
<dependency>
	<groupId>io.codemine.java.postgresql</groupId>
	<artifactId>jdbc</artifactId>
	<version>0.1.0</version>
</dependency>
```

This module depends on `postgresql-codecs` and the official `org.postgresql` driver.

## Usage with JDBC (pgjdbc)

Encode a value with a codec and wrap it into a `PGobject` so the driver
sends the correct type annotation:

```java
import io.codemine.java.postgresql.codecs.Codec;
import org.postgresql.util.PGobject;

Codec<Integer> codec = Codec.INT4;

PGobject obj = new PGobject();
obj.setType(codec.typeSig()); // e.g. "int4"
obj.setValue(codec.encodeInTextToString(42));

PreparedStatement ps = connection.prepareStatement("INSERT INTO t (col) VALUES (?)");
ps.setObject(1, obj);
ps.executeUpdate();
```

Decoding text columns encoded this way is just as easy:

```java
String text = rs.getString("col");
Integer value = codec.decodeInTextFromString(text);
```

Array/Composite support is automatic when codecs are composed into
arrays or composite codecs.

## Build & test

Build using the provided script or Maven:

```bash
./build.bash
# or
mvn clean verify
```

Integration tests use Testcontainers and therefore require Docker to be
available on the host when running the full test suite.
