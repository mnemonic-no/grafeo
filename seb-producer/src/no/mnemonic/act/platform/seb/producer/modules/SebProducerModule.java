package no.mnemonic.act.platform.seb.producer.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.seb.model.v1.FactSEB;
import no.mnemonic.act.platform.seb.producer.v1.FactProducer;
import no.mnemonic.act.platform.seb.producer.v1.providers.FactKafkaDestinationProvider;
import no.mnemonic.messaging.documentchannel.DocumentDestination;

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
