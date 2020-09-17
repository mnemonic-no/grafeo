package no.mnemonic.act.platform.seb.producer.v1.providers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import no.mnemonic.act.platform.seb.model.v1.FactSEB;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import org.apache.kafka.common.serialization.Serializer;

import java.io.UncheckedIOException;
import java.util.Map;

class FactKafkaSerializer implements Serializer<FactSEB> {

  private static final Logger LOGGER = Logging.getLogger(FactKafkaSerializer.class);
  private static final ObjectMapper MAPPER = JsonMapper.builder().build();
  private static final ObjectWriter WRITER = MAPPER.writerFor(FactSEB.class);

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    // Noop
  }

  @Override
  public byte[] serialize(String topic, FactSEB data) {
    try {
      return WRITER.writeValueAsBytes(data);
    } catch (JsonProcessingException ex) {
      LOGGER.error(ex, "Failed to serialize FactSEB.");
      throw new UncheckedIOException("Failed to serialize FactSEB.", ex);
    }
  }

  @Override
  public void close() {
    // Noop
  }
}
