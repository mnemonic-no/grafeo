package no.mnemonic.services.grafeo.seb.esengine.v1.consumers;

import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.services.common.hazelcast.consumer.TransactionalConsumer;
import no.mnemonic.services.grafeo.dao.elastic.FactSearchManager;
import no.mnemonic.services.grafeo.dao.elastic.document.FactDocument;
import no.mnemonic.services.grafeo.seb.esengine.v1.converters.FactConverter;
import no.mnemonic.services.grafeo.seb.model.v1.FactSEB;

import jakarta.inject.Inject;
import java.util.Collection;

import static no.mnemonic.services.grafeo.dao.elastic.FactSearchManager.TargetIndex.Daily;
import static no.mnemonic.services.grafeo.dao.elastic.FactSearchManager.TargetIndex.TimeGlobal;

/**
 * Component which consumes {@link FactSEB} models, converts them to {@link FactDocument}, and indexes them into ElasticSearch.
 */
public class FactConsumer implements TransactionalConsumer<FactSEB> {

  private static final Logger LOGGER = Logging.getLogger(FactConsumer.class);

  private final FactSearchManager factSearchManager;
  private final FactConverter factConverter;

  @Inject
  public FactConsumer(FactSearchManager factSearchManager, FactConverter factConverter) {
    this.factSearchManager = factSearchManager;
    this.factConverter = factConverter;
  }

  @Override
  public void consume(Collection<FactSEB> items) {
    if (CollectionUtils.isEmpty(items)) return;

    for (FactSEB seb : items) {
      FactDocument document = factConverter.apply(seb);
      if (document != null) {
        LOGGER.debug("Indexing Fact with id = %s into ElasticSearch.", document.getId());
        if (seb.isSet(FactSEB.Flag.TimeGlobalIndex)) {
          factSearchManager.indexFact(document, TimeGlobal);
        } else {
          factSearchManager.indexFact(document, Daily);
        }
      }
    }
  }

  @Override
  public void close() {
    // Noop
  }
}
