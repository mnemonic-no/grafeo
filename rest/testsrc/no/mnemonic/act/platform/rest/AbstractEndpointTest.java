package no.mnemonic.act.platform.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;

import static org.mockito.Mockito.mock;

public class AbstractEndpointTest extends JerseyTest {

  private final static ObjectMapper mapper = new ObjectMapper();
  private final ThreatIntelligenceService tiService = mock(ThreatIntelligenceService.class);

  protected ThreatIntelligenceService getTiService() {
    return tiService;
  }

  protected JsonNode getPayload(Response response) throws IOException {
    // Return the payload in the "data" field of the returned ResultStash.
    return mapper.readTree(response.readEntity(String.class)).get("data");
  }

  @Override
  protected Application configure() {
    return new ResourceConfig()
            .packages(true, "no.mnemonic.act.platform.rest.api")
            .register(JacksonJaxbJsonProvider.class, MessageBodyReader.class, MessageBodyWriter.class)
            .register(new AbstractBinder() {
              @Override
              protected void configure() {
                // Add more service injections here.
                bind(tiService).to(ThreatIntelligenceService.class);
              }
            });
  }

}
