package io.codemine.java.postgresql.jdbc;

public class JsonCodecIT extends CodecITBase<com.fasterxml.jackson.databind.JsonNode> {
  public JsonCodecIT() {
    super(Codec.JSON);
  }
}
