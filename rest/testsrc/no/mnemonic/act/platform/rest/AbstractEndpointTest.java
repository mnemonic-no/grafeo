package no.mnemonic.act.platform.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.rest.container.ApiServer;
import no.mnemonic.commons.testtools.AvailablePortFinder;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.mockito.MockitoAnnotations.initMocks;

public class AbstractEndpointTest {

  private final static ObjectMapper mapper = new ObjectMapper();
  private final static int port = AvailablePortFinder.getAvailablePort(9000);

  @Mock
  private ThreatIntelligenceService tiService;

  private ApiServer server;

  @Before
  public void initialize() {
    initMocks(this);
    Injector injector = Guice.createInjector(new TestRestModule());
    server = injector.getInstance(ApiServer.class);
    server.startComponent();
  }

  @After
  public void shutdown() {
    server.stopComponent();
  }

  protected ThreatIntelligenceService getTiService() {
    return tiService;
  }

  protected WebTarget target(String url) {
    return ClientBuilder.newClient().target("http://localhost:" + port + url);
  }

  protected JsonNode getPayload(Response response) throws IOException {
    // Return the payload in the "data" field of the returned ResultStash.
    return mapper.readTree(response.readEntity(String.class)).get("data");
  }

  private class TestRestModule extends AbstractModule {

    @Override
    protected void configure() {
      install(new RestModule());
      bind(ThreatIntelligenceService.class).toInstance(tiService);
      bind(String.class).annotatedWith(Names.named("api.server.port")).toInstance(String.valueOf(port));
    }

  }

}
