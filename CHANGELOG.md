# Upcoming

## Breaking

- Removed `StatementBatch.of(Statement...)`; construct with the `Iterable` constructor instead.
- `StatementBatch.execute(Connection)` now returns `List<R>` instead of `ArrayList<R>`.

## Non-breaking

- Added `Statement.map(Function)` to transform a statement's result type while keeping its SQL, parameter binding and metadata.
- Added `Statement.statementName()`, `operationName()`, `collectionName()` metadata defaults for spans/logging.
- Added `Statement.idempotent()`, defaulting to `false`, to mark statements safe to retry.
- Added `StatementBatch.sql()` and `StatementBatch.size()` accessors.
- Added a `StatementBatch.execute(Connection, int queryTimeoutSeconds)` overload.

# v0.6.0

## Breaking

- Migrated to `postgresql-codecs` 0.3.1.
  - `Codec.OID` type changed from `Codec<Integer>` to `Codec<Long>`.
  - Replaced `Codec.MONEY` with `Codec.money(int decimals)` returning `Codec<BigDecimal>`.
