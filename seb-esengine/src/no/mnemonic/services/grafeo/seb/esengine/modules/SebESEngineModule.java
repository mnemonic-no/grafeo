package no.mnemonic.services.grafeo.seb.esengine.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import no.mnemonic.messaging.documentchannel.DocumentSource;
import no.mnemonic.services.common.hazelcast.consumer.TransactionalConsumer;
import no.mnemonic.services.grafeo.seb.esengine.v1.consumers.FactConsumer;
import no.mnemonic.services.grafeo.seb.esengine.v1.handlers.FactHazelcastToElasticSearchHandler;
import no.mnemonic.services.grafeo.seb.esengine.v1.handlers.FactKafkaToHazelcastHandler;
import no.mnemonic.services.grafeo.seb.esengine.v1.providers.FactKafkaSourceProvider;
import no.mnemonic.services.grafeo.seb.model.v1.FactSEB;

public class SebESEngineModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(new TypeLiteral<DocumentSource<FactSEB>>() {})
            .toProvider(FactKafkaSourceProvider.class)
            .in(Scopes.SINGLETON);
    bind(FactKafkaToHazelcastHandler.class);
    bind(FactHazelcastToElasticSearchHandler.class);
    bind(new TypeLiteral<TransactionalConsumer<FactSEB>>() {}).to(FactConsumer.class);
  }
}
