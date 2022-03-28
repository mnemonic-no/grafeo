package no.mnemonic.act.platform.seb.esengine.v1.consumers;

import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.seb.esengine.v1.converters.FactConverter;
import no.mnemonic.act.platform.seb.model.v1.FactSEB;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.services.common.hazelcast.consumer.TransactionalConsumer;

import javax.inject.Inject;
import java.util.Collection;

import static no.mnemonic.act.platform.dao.elastic.FactSearchManager.TargetIndex.*;

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

        // Compatibility: For now always index into the legacy 'act' index in addition to time global or daily indices.
        factSearchManager.indexFact(document, Legacy);
      }
    }
  }

  @Override
  public void close() {
    // Noop
  }
}
