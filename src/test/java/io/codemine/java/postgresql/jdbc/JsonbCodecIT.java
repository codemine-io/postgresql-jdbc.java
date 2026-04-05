package io.codemine.java.postgresql.jdbc;

public class JsonbCodecIT extends CodecITBase<com.fasterxml.jackson.databind.JsonNode> {
  public JsonbCodecIT() {
    super(Codec.JSONB);
  }
}
