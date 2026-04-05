package io.codemine.java.postgresql.jdbc;

import io.codemine.java.postgresql.codecs.Multirange;

public class Int8MultirangeCodecIT extends CodecITBase<Multirange<Long>> {
  public Int8MultirangeCodecIT() {
    super(Codec.INT8MULTIRANGE);
  }
}
