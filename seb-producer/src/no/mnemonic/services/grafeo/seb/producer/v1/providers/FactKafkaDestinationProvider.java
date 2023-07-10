package no.mnemonic.services.grafeo.seb.producer.v1.providers;

import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.messaging.documentchannel.DocumentDestination;
import no.mnemonic.messaging.documentchannel.kafka.KafkaDocumentDestination;
import no.mnemonic.messaging.documentchannel.kafka.KafkaProducerProvider;
import no.mnemonic.services.grafeo.seb.model.v1.FactSEB;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class FactKafkaDestinationProvider implements LifecycleAspect, Provider<DocumentDestination<FactSEB>> {

  private final AtomicReference<DocumentDestination<FactSEB>> instance = new AtomicReference<>();

  private final int port;
  private final String contactPoints;
  private final String producerTopic;
  private final boolean producerEnabled;

  @Inject
  public FactKafkaDestinationProvider(
          @Named("grafeo.seb.kafka.port") int port,
          @Named("grafeo.seb.kafka.contact.points") String contactPoints,
          @Named("grafeo.seb.kafka.producer.topic") String producerTopic,
          @Named("grafeo.seb.kafka.producer.enabled") boolean producerEnabled) {
    this.port = port;
    this.contactPoints = contactPoints;
    this.producerTopic = producerTopic;
    this.producerEnabled = producerEnabled;
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
  public DocumentDestination<FactSEB> get() {
    return instance.updateAndGet(existing -> {
      if (existing != null) return existing;
      return createKafkaDocumentDestination();
    });
  }

  private KafkaDocumentDestination<FactSEB> createKafkaDocumentDestination() {
    return KafkaDocumentDestination.<FactSEB>builder()
            .setProducerProvider(createKafkaProducerProvider())
            .setType(FactSEB.class)
            .setTopicName(producerTopic)
            .setDisabled(!producerEnabled)
            .build();
  }

  private KafkaProducerProvider createKafkaProducerProvider() {
    return KafkaProducerProvider.builder()
            .setKafkaHosts(contactPoints)
            .setKafkaPort(port)
            .addSerializer(FactSEB.class, new FactKafkaSerializer())
            .build();
  }
}
