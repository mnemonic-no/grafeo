package no.mnemonic.act.platform.dao.elastic;

import no.mnemonic.act.platform.entity.handlers.EntityHandler;
import no.mnemonic.commons.junit.docker.ElasticSearchDockerResource;
import org.junit.*;
import org.mockito.Mock;

import java.util.UUID;
import java.util.function.Function;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public abstract class AbstractManagerTest {

  private static ClientFactory clientFactory;
  private FactSearchManager factSearchManager;

  @Mock
  private Function<UUID, EntityHandler> entityHandlerForTypeIdResolver;
  @Mock
  private EntityHandler entityHandler;

  @ClassRule
  public static ElasticSearchDockerResource elastic = ElasticSearchDockerResource.builder()
          // Need to specify the exact version here because Elastic doesn't publish images with the 'latest' tag.
          // Usually this should be the same version as the ElasticSearch client used.
          .setImageName("elasticsearch/elasticsearch:5.6.4")
          .addApplicationPort(9200)
          .build();

  @BeforeClass
  public static void setup() {
    clientFactory = ClientFactory.builder()
            .setPort(elastic.getExposedHostPort(9200))
            .addContactPoint("127.0.0.1")
            .build();
    clientFactory.startComponent();
  }

  @Before
  public void initialize() {
    initMocks(this);

    when(entityHandlerForTypeIdResolver.apply(any())).thenReturn(entityHandler);
    when(entityHandler.encode(any())).then(returnsFirstArg());
    when(entityHandler.decode(any())).then(returnsFirstArg());

    factSearchManager = new FactSearchManager(clientFactory, entityHandlerForTypeIdResolver);
    factSearchManager.startComponent();
  }

  @After
  public void cleanup() {
    elastic.deleteIndices();
  }

  @AfterClass
  public static void teardown() {
    clientFactory.stopComponent();
  }

  protected FactSearchManager getFactSearchManager() {
    return factSearchManager;
  }

  protected EntityHandler getEntityHandler() {
    return entityHandler;
  }

}
