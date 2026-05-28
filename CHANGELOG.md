# Upcoming

## Breaking

- Migrated to `postgresql-codecs` 0.3.1.
  - `Codec.OID` type changed from `Codec<Integer>` to `Codec<Long>`.
  - `Codec.MONEY` type changed from `Codec<Long>` to `Codec<BigDecimal>`.
