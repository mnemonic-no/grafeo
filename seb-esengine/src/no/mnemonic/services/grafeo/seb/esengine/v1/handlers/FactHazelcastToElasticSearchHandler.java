package no.mnemonic.services.grafeo.seb.esengine.v1.handlers;

import com.hazelcast.core.HazelcastInstance;
import no.mnemonic.services.common.hazelcast.consumer.HazelcastTransactionalConsumerHandler;
import no.mnemonic.services.common.hazelcast.consumer.TransactionalConsumer;
import no.mnemonic.services.grafeo.seb.model.v1.FactSEB;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import static no.mnemonic.services.grafeo.seb.esengine.v1.handlers.FactKafkaToHazelcastHandler.FACT_HAZELCAST_QUEUE_NAME;

/**
 * Component which fetches {@link FactSEB} models from a Hazelcast queue and indexes them into ElasticSearch.
 */
@Singleton
public class FactHazelcastToElasticSearchHandler extends HazelcastTransactionalConsumerHandler<FactSEB> {

  private static final int WORKER_COUNT = 4;

  @Inject
  public FactHazelcastToElasticSearchHandler(HazelcastInstance hazelcastInstance, Provider<TransactionalConsumer<FactSEB>> consumerProvider) {
    super(hazelcastInstance, FACT_HAZELCAST_QUEUE_NAME, consumerProvider);
    super.setWorkerCount(WORKER_COUNT);
  }
}
