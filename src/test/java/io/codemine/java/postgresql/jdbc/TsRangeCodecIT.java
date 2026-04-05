package io.codemine.java.postgresql.jdbc;

import io.codemine.java.postgresql.codecs.Range;

public class TsRangeCodecIT extends CodecITBase<Range<java.time.LocalDateTime>> {
  public TsRangeCodecIT() {
    super(Codec.TSRANGE);
  }
}
