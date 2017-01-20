package no.mnemonic.act.platform.service.ti;

import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;

import javax.inject.Inject;

public class ThreatIntelligenceServiceImpl implements ThreatIntelligenceService {

  private final FactManager factManager;
  private final ObjectManager objectManager;

  @Inject
  public ThreatIntelligenceServiceImpl(FactManager factManager, ObjectManager objectManager) {
    this.factManager = factManager;
    this.objectManager = objectManager;
  }

}
