package io.codemine.java.postgresql.jdbc;

import io.codemine.java.postgresql.codecs.Range;

public class DateRangeCodecIT extends CodecITBase<Range<java.time.LocalDate>> {
  public DateRangeCodecIT() {
    super(Codec.DATERANGE);
  }
}
