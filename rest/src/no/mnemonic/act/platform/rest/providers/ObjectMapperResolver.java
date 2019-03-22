package no.mnemonic.act.platform.rest.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import no.mnemonic.act.platform.rest.api.ResultStash;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class ObjectMapperResolver implements ContextResolver<ObjectMapper> {

  private static final ObjectMapper mapper = new ObjectMapper();

  static {
    // Register custom ResultStashSerializer used to serialize ResultStash.
    SimpleModule extensions = new SimpleModule();
    extensions.addSerializer(ResultStash.class, new ResultStash.ResultStashSerializer());
    mapper.registerModule(extensions);
  }

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return mapper;
  }

}
