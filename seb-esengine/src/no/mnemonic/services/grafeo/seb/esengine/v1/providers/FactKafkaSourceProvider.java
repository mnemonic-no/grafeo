package no.mnemonic.services.grafeo.seb.esengine.v1.providers;

import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.messaging.documentchannel.DocumentSource;
import no.mnemonic.messaging.documentchannel.kafka.KafkaConsumerProvider;
import no.mnemonic.messaging.documentchannel.kafka.KafkaDocumentSource;
import no.mnemonic.messaging.documentchannel.noop.NullDocumentSource;
import no.mnemonic.services.grafeo.seb.model.v1.FactSEB;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class FactKafkaSourceProvider implements LifecycleAspect, Provider<DocumentSource<FactSEB>> {

  private final AtomicReference<DocumentSource<FactSEB>> instance = new AtomicReference<>();

  private final int port;
  private final String contactPoints;
  private final String consumerTopics;
  private final String consumerGroup;
  private final boolean consumerEnabled;

  @Inject
  public FactKafkaSourceProvider(
          @Named("act.seb.kafka.port") int port,
          @Named("act.seb.kafka.contact.points") String contactPoints,
          @Named("act.seb.kafka.esengine.consumer.topics") String consumerTopics,
          @Named("act.seb.kafka.esengine.consumer.group") String consumerGroup,
          @Named("act.seb.kafka.esengine.consumer.enabled") boolean consumerEnabled) {
    this.port = port;
    this.contactPoints = contactPoints;
    this.consumerTopics = consumerTopics;
    this.consumerGroup = consumerGroup;
    this.consumerEnabled = consumerEnabled;
  }

  @Override
  public void startComponent() {
    get(); // Force initialization on startup.
  }

  @Override
  public void stopComponent() {
    // Close channel cleanly on shutdown.
    instance.updateAndGet(existing -> {
      if (existing != null) existing.close();
      return null;
    });
  }

  @Override
  public DocumentSource<FactSEB> get() {
    return instance.updateAndGet(existing -> {
      if (existing != null) return existing;
      return createDocumentSource();
    });
  }

  private DocumentSource<FactSEB> createDocumentSource() {
    if (!consumerEnabled) return new NullDocumentSource<>();

    return KafkaDocumentSource.<FactSEB>builder()
            .setConsumerProvider(createKafkaConsumerProvider())
            .setType(FactSEB.class)
            .setTopicName(ListUtils.list(consumerTopics.split(",")))
            .build();
  }

  private KafkaConsumerProvider createKafkaConsumerProvider() {
    return KafkaConsumerProvider.builder()
            .setGroupID(consumerGroup)
            .setKafkaHosts(contactPoints)
            .setKafkaPort(port)
            .addDeserializer(FactSEB.class, new FactKafkaDeserializer())
            .build();
  }
}
