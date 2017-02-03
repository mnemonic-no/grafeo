package no.mnemonic.act.platform.service.ti;

import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.service.Service;
import no.mnemonic.act.platform.service.contexts.SecurityContext;

import javax.inject.Inject;

public class ThreatIntelligenceServiceImpl implements Service, ThreatIntelligenceService {

  private final FactManager factManager;
  private final ObjectManager objectManager;

  @Inject
  public ThreatIntelligenceServiceImpl(FactManager factManager, ObjectManager objectManager) {
    this.factManager = factManager;
    this.objectManager = objectManager;
  }

  @Override
  public SecurityContext createSecurityContext() {
    return new SecurityContext();
  }

}
