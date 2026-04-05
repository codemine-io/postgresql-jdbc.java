package io.codemine.java.postgresql.jdbc;

import io.codemine.java.postgresql.codecs.Multirange;

public class DateMultirangeCodecIT extends CodecITBase<Multirange<java.time.LocalDate>> {
  public DateMultirangeCodecIT() {
    super(Codec.DATEMULTIRANGE);
  }
}
