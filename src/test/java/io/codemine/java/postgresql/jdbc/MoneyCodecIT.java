package io.codemine.java.postgresql.jdbc;

public class MoneyCodecIT extends CodecITBase<java.math.BigDecimal> {
  public MoneyCodecIT() {
    super(Codec.MONEY);
  }
}
