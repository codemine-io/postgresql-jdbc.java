# v0.6.0

## Breaking

- Split `TransactionContext` into `ExecutionContext` (safe operations inside a transaction body)
  and `TransactionContext extends ExecutionContext` (boundary control).
- Changed `Transaction.run` parameter from `TransactionContext` to `ExecutionContext`.
- Deleted `StatementBatch`; use `ExecutionContext.executeBatch(Iterable<? extends Statement<R>>)`
  instead.
- Changed `executeBatch` return type from `ArrayList<R>` to `List<R>`.
- Migrated to `postgresql-codecs` 0.3.1.
  - `Codec.OID` type changed from `Codec<Integer>` to `Codec<Long>`.
  - Replaced `Codec.MONEY` with `Codec.money(int decimals)` returning `Codec<BigDecimal>`.
