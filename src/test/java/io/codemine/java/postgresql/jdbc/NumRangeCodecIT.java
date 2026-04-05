package io.codemine.java.postgresql.jdbc;

import io.codemine.java.postgresql.codecs.Range;

public class NumRangeCodecIT extends CodecITBase<Range<java.math.BigDecimal>> {
  public NumRangeCodecIT() {
    super(Codec.NUMRANGE);
  }
}
