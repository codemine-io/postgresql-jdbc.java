package io.codemine.java.postgresql.jdbc;

public class TimestampCodecIT extends CodecITBase<java.time.LocalDateTime> {
  public TimestampCodecIT() {
    super(Codec.TIMESTAMP);
  }
}
