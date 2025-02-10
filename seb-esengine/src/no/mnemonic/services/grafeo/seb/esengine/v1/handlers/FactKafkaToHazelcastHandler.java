package no.mnemonic.services.grafeo.seb.esengine.v1.handlers;

import com.hazelcast.core.HazelcastInstance;
import no.mnemonic.messaging.documentchannel.DocumentSource;
import no.mnemonic.services.common.hazelcast.consumer.KafkaToHazelcastHandler;
import no.mnemonic.services.grafeo.seb.model.v1.FactSEB;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Component which fetches {@link FactSEB} models from Kafka and forwards them to a Hazelcast queue.
 */
@Singleton
public class FactKafkaToHazelcastHandler extends KafkaToHazelcastHandler<FactSEB> {

  public static final String FACT_HAZELCAST_QUEUE_NAME = "ACT.ESEngine.Queue.Fact";

  @Inject
  public FactKafkaToHazelcastHandler(DocumentSource<FactSEB> source, HazelcastInstance hazelcastInstance) {
    super(source, hazelcastInstance, FACT_HAZELCAST_QUEUE_NAME);
  }
}
