package no.mnemonic.services.grafeo.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import no.mnemonic.commons.testtools.AvailablePortFinder;
import no.mnemonic.services.grafeo.api.service.v1.GrafeoService;
import no.mnemonic.services.grafeo.rest.container.ApiServer;
import no.mnemonic.services.grafeo.rest.modules.GrafeoRestModule;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.mockito.MockitoAnnotations.initMocks;

public abstract class AbstractEndpointTest {

  private final static ObjectMapper mapper = JsonMapper.builder().build();
  private final static int port = AvailablePortFinder.getAvailablePort(9000);

  @Mock
  private GrafeoService service;

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

  protected GrafeoService getService() {
    return service;
  }

  protected WebTarget target(String url) {
    return ClientBuilder.newClient().target("http://localhost:" + port + url);
  }

  protected JsonNode getPayload(Response response) throws IOException {
    // Return the payload in the "data" field of the returned ResultStash.
    return mapper.readTree(response.readEntity(String.class)).get("data");
  }

  protected ArrayNode getMessages(Response response) throws IOException {
    // Return the "messages" field of the returned ResultStash.
    return (ArrayNode) mapper.readTree(response.readEntity(String.class)).get("messages");
  }

  private class TestRestModule extends AbstractModule {

    @Override
    protected void configure() {
      install(new GrafeoRestModule());
      bind(GrafeoService.class).toInstance(service);
      bind(String.class).annotatedWith(Names.named("act.api.server.port")).toInstance(String.valueOf(port));
      bind(String.class).annotatedWith(Names.named("act.api.cors.allowed.origins")).toInstance("http://www.example.org");
    }

  }

}
