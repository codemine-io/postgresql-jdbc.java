package io.codemine.java.postgresql.jdbc;

import io.codemine.java.postgresql.codecs.Multirange;

public class TsMultirangeCodecIT extends CodecITBase<Multirange<java.time.LocalDateTime>> {
  public TsMultirangeCodecIT() {
    super(Codec.TSMULTIRANGE);
  }
}
