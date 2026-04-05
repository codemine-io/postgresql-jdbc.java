package io.codemine.java.postgresql.jdbc;

public class TimeCodecIT extends CodecITBase<java.time.LocalTime> {
  public TimeCodecIT() {
    super(Codec.TIME);
  }
}
