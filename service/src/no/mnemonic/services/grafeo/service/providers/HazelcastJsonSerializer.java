package no.mnemonic.services.grafeo.service.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hazelcast.nio.serialization.ByteArraySerializer;
import com.hazelcast.nio.serialization.Serializer;

import java.io.IOException;

/**
 * A Hazelcast {@link Serializer} implementation which serializes data as JSON.
 *
 * @param <T> Type of data
 */
public class HazelcastJsonSerializer<T> implements ByteArraySerializer<T> {

  private static final ObjectMapper MAPPER = JsonMapper.builder().build();

  private final ObjectWriter writer;
  private final ObjectReader reader;
  private final int typeID;

  public HazelcastJsonSerializer(Class<T> type, int typeID) {
    this.writer = MAPPER.writerFor(type);
    this.reader = MAPPER.readerFor(type);
    this.typeID = typeID;
  }

  @Override
  public byte[] write(T object) throws IOException {
    return writer.writeValueAsBytes(object);
  }

  @Override
  public T read(byte[] buffer) throws IOException {
    if (buffer == null) return null;
    return reader.readValue(buffer);
  }

  @Override
  public int getTypeId() {
    return typeID;
  }

  @Override
  public void destroy() {
    // Noop
  }
}
