package no.mnemonic.services.grafeo.rest.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import no.mnemonic.services.grafeo.rest.api.ResultStash;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class ObjectMapperResolver implements ContextResolver<ObjectMapper> {

  private static final ObjectMapper mapper;

  static {
    // Register custom ResultStashSerializer used to serialize ResultStash.
    SimpleModule extensions = new SimpleModule();
    extensions.addSerializer(ResultStash.class, new ResultStash.ResultStashSerializer());
    mapper = JsonMapper.builder()
            .addModule(extensions)
            .build();
  }

  public static ObjectMapper getInstance() {
    return mapper;
  }

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return mapper;
  }

}
