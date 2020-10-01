package no.mnemonic.act.platform.service.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hazelcast.nio.serialization.ByteArraySerializer;
import no.mnemonic.act.platform.seb.model.v1.FactSEB;

import java.io.IOException;

class HazelcastFactSebSerializer implements ByteArraySerializer<FactSEB> {

  private static final ObjectMapper MAPPER = JsonMapper.builder().build();
  private static final ObjectWriter WRITER = MAPPER.writerFor(FactSEB.class);
  private static final ObjectReader READER = MAPPER.readerFor(FactSEB.class);

  @Override
  public byte[] write(FactSEB object) throws IOException {
    return WRITER.writeValueAsBytes(object);
  }

  @Override
  public FactSEB read(byte[] buffer) throws IOException {
    if (buffer == null) return null;
    return READER.readValue(buffer);
  }

  @Override
  public int getTypeId() {
    return 46616374; // Type ID just needs to be unique.
  }

  @Override
  public void destroy() {
    // Noop
  }
}
