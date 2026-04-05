package io.codemine.java.postgresql.jdbc;

import io.codemine.java.postgresql.codecs.Multirange;

public class NumMultirangeCodecIT extends CodecITBase<Multirange<java.math.BigDecimal>> {
  public NumMultirangeCodecIT() {
    super(Codec.NUMMULTIRANGE);
  }
}
