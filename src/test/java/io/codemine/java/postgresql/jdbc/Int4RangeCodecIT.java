package io.codemine.java.postgresql.jdbc;

import io.codemine.java.postgresql.codecs.Range;

public class Int4RangeCodecIT extends CodecITBase<Range<Integer>> {
  public Int4RangeCodecIT() {
    super(Codec.INT4RANGE);
  }
}
