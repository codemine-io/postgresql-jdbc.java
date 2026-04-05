package io.codemine.java.postgresql.jdbc;

import io.codemine.java.postgresql.codecs.Range;

public class Int8RangeCodecIT extends CodecITBase<Range<Long>> {
  public Int8RangeCodecIT() {
    super(Codec.INT8RANGE);
  }
}
