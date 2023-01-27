package no.mnemonic.act.platform.test.integration;

import com.google.inject.*;
import com.google.inject.name.Names;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.seb.esengine.v1.consumers.FactConsumer;
import no.mnemonic.act.platform.seb.esengine.v1.handlers.FactHazelcastToElasticSearchHandler;
import no.mnemonic.act.platform.seb.esengine.v1.handlers.FactKafkaToHazelcastHandler;
import no.mnemonic.act.platform.seb.model.v1.FactSEB;
import no.mnemonic.act.platform.service.providers.HazelcastInstanceProvider;
import no.mnemonic.commons.metrics.MetricException;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.lambda.LambdaUtils;
import no.mnemonic.messaging.documentchannel.DocumentBatch;
import no.mnemonic.messaging.documentchannel.DocumentSource;
import no.mnemonic.services.common.hazelcast.consumer.TransactionalConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ESEngineIT {

  @Mock
  private DocumentSource<FactSEB> documentSource;
  @Mock
  private FactSearchManager factSearchManager;

  private FactKafkaToHazelcastHandler kafkaToHzHandler;
  private FactHazelcastToElasticSearchHandler hzToEsHandler;

  @Before
  public void setUp() {
    initMocks(this);

    Injector injector = Guice.createInjector(new TestModule());
    kafkaToHzHandler = injector.getInstance(FactKafkaToHazelcastHandler.class);
    hzToEsHandler = injector.getInstance(FactHazelcastToElasticSearchHandler.class);
  }

  @After
  public void cleanUp() {
    ObjectUtils.ifNotNullDo(hzToEsHandler, FactHazelcastToElasticSearchHandler::stopComponent);
    ObjectUtils.ifNotNullDo(kafkaToHzHandler, FactKafkaToHazelcastHandler::stopComponent);
    Hazelcast.shutdownAll();
  }

  @Test
  public void testESEnginePipeline() throws Exception {
    FactSEB fact = FactSEB.builder()
            .setId(UUID.randomUUID())
            .build();

    // This simulates a DocumentSource being called by the Kafka to Hazelcast handler.
    when(documentSource.poll(any()))
            .thenReturn(new FixedSizeDocumentBatch(Collections.singleton(fact)))
            .thenReturn(new FixedSizeDocumentBatch(Collections.emptySet()));

    kafkaToHzHandler.startComponent();
    hzToEsHandler.startComponent();

    // Wait up to 10s for the Fact to be processed by the Hazelcast to ElasticSearch handler.
    // Otherwise consider the test failed. This is required because the pipeline is asynchronous.
    if (!LambdaUtils.waitFor(this::factProcessed, 10, TimeUnit.SECONDS)) {
      fail("Test of ES engine pipeline failed.");
    }

    // Verify that Hazelcast to ElasticSearch handler has called the DAO to index the Fact.
    verify(factSearchManager).indexFact(argThat(doc -> Objects.equals(fact.getId(), doc.getId())), notNull());
  }

  private boolean factProcessed() {
    try {
      // Once the Fact is processed successfully the handler updates its metric.
      return Objects.equals(1L, hzToEsHandler.getMetrics().getData("item.submit.count").longValue());
    } catch (MetricException ex) {
      return false;
    }
  }

  private static class FixedSizeDocumentBatch implements DocumentBatch<FactSEB> {

    private final Collection<FactSEB> documents;

    private FixedSizeDocumentBatch(Collection<FactSEB> documents) {
      this.documents = documents;
    }

    @Override
    public Collection<FactSEB> getDocuments() {
      return documents;
    }

    @Override
    public void acknowledge() {
    }

    @Override
    public void reject() {
    }
  }

  private class TestModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(String.class).annotatedWith(Names.named("act.hazelcast.instance.name")).toInstance(UUID.randomUUID().toString());
      bind(String.class).annotatedWith(Names.named("act.hazelcast.group.name")).toInstance(UUID.randomUUID().toString());
      bind(String.class).annotatedWith(Names.named("act.hazelcast.multicast.address")).toInstance("224.2.2.3");
      bind(String.class).annotatedWith(Names.named("act.hazelcast.multicast.port")).toInstance("54327");
      bind(String.class).annotatedWith(Names.named("act.hazelcast.multicast.enabled")).toInstance("false");

      bind(HazelcastInstance.class).toProvider(HazelcastInstanceProvider.class).in(Scopes.SINGLETON);
      bind(new TypeLiteral<TransactionalConsumer<FactSEB>>() {}).to(FactConsumer.class);
      bind(new TypeLiteral<DocumentSource<FactSEB>>() {}).toInstance(documentSource);
      bind(FactSearchManager.class).toInstance(factSearchManager);
    }
  }
}
