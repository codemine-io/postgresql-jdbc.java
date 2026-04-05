package io.codemine.java.postgresql.jdbc;

public class NumericCodecIT extends CodecITBase<java.math.BigDecimal> {
  public NumericCodecIT() {
    super(Codec.NUMERIC);
  }
}
