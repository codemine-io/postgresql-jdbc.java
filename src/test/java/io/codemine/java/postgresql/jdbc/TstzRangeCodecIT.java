package io.codemine.java.postgresql.jdbc;

import io.codemine.java.postgresql.codecs.Range;

public class TstzRangeCodecIT extends CodecITBase<Range<java.time.Instant>> {
  public TstzRangeCodecIT() {
    super(Codec.TSTZRANGE);
  }
}
