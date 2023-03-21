package no.mnemonic.services.grafeo.seb.producer.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import no.mnemonic.messaging.documentchannel.DocumentDestination;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.seb.model.v1.FactSEB;
import no.mnemonic.services.grafeo.seb.producer.v1.FactProducer;
import no.mnemonic.services.grafeo.seb.producer.v1.providers.FactKafkaDestinationProvider;

import java.util.function.Consumer;

public class SebProducerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(new TypeLiteral<DocumentDestination<FactSEB>>() {})
            .toProvider(FactKafkaDestinationProvider.class)
            .in(Scopes.SINGLETON);
    bind(new TypeLiteral<Consumer<FactRecord>>() {}).to(FactProducer.class);
  }
}
