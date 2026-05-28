# v0.6.0

## Breaking

- Migrated to `postgresql-codecs` 0.3.1.
  - `Codec.OID` type changed from `Codec<Integer>` to `Codec<Long>`.
  - Replaced `Codec.MONEY` with `Codec.money(int decimals)` returning `Codec<BigDecimal>`.
