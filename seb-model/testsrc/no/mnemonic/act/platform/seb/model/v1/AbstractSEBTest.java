package no.mnemonic.act.platform.seb.model.v1;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.BeforeClass;

abstract class AbstractSEBTest {

  private static ObjectMapper mapper;

  @BeforeClass
  public static void setUp() {
    // Initialize object mapper for testing. With this configuration we save a lot of quotes and escaping when creating JSON strings.
    mapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();
  }

  protected ObjectMapper getMapper() {
    return mapper;
  }
}
