package no.mnemonic.act.platform.seb.esengine.v1.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import no.mnemonic.act.platform.seb.model.v1.FactSEB;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

class FactKafkaDeserializer implements Deserializer<FactSEB> {

  private static final Logger LOGGER = Logging.getLogger(FactKafkaDeserializer.class);
  private static final ObjectMapper MAPPER = JsonMapper.builder().build();
  private static final ObjectReader READER = MAPPER.readerFor(FactSEB.class);

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    // Noop
  }

  @Override
  public FactSEB deserialize(String topic, byte[] data) {
    if (data == null) return null;

    try {
      return READER.readValue(data);
    } catch (IOException ex) {
      LOGGER.error(ex, "Failed to deserialize FactSEB.");
      throw new UncheckedIOException("Failed to deserialize FactSEB.", ex);
    }
  }

  @Override
  public void close() {
    // Noop
  }
}
