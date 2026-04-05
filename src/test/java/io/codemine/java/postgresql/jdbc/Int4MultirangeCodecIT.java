package io.codemine.java.postgresql.jdbc;

import io.codemine.java.postgresql.codecs.Multirange;

public class Int4MultirangeCodecIT extends CodecITBase<Multirange<Integer>> {
  public Int4MultirangeCodecIT() {
    super(Codec.INT4MULTIRANGE);
  }
}
