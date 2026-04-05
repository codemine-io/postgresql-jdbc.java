package io.codemine.java.postgresql.jdbc;

import io.codemine.java.postgresql.codecs.Multirange;

public class TstzMultirangeCodecIT extends CodecITBase<Multirange<java.time.Instant>> {
  public TstzMultirangeCodecIT() {
    super(Codec.TSTZMULTIRANGE);
  }
}
