package no.mnemonic.services.grafeo.seb.producer.v1;

import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.messaging.documentchannel.DocumentDestination;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.seb.model.v1.FactSEB;
import no.mnemonic.services.grafeo.seb.producer.v1.converters.FactConverter;

import javax.inject.Inject;
import java.util.function.Consumer;

/**
 * Component which takes a {@link FactRecord}, converts it to a {@link FactSEB} model, and writes it to a Kafka topic.
 */
public class FactProducer implements Consumer<FactRecord> {

  private static final Logger LOGGER = Logging.getLogger(FactProducer.class);

  private final FactConverter converter;
  private final DocumentDestination<FactSEB> destination;

  @Inject
  public FactProducer(FactConverter converter, DocumentDestination<FactSEB> destination) {
    this.converter = converter;
    this.destination = destination;
  }

  @Override
  public void accept(FactRecord fact) {
    FactSEB seb = converter.apply(fact);
    if (seb != null) {
      LOGGER.debug("Sending Fact with id = %s to document channel.", seb.getId());
      destination.getDocumentChannel().sendDocument(seb);
    }
  }
}
