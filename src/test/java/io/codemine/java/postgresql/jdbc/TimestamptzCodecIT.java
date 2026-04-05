package io.codemine.java.postgresql.jdbc;

public class TimestamptzCodecIT extends CodecITBase<java.time.Instant> {
  public TimestamptzCodecIT() {
    super(Codec.TIMESTAMPTZ);
  }
}
