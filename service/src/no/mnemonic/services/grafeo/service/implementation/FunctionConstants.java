package no.mnemonic.services.grafeo.service.implementation;

import no.mnemonic.services.common.auth.model.NamedFunction;

public enum FunctionConstants implements NamedFunction {
  addThreatIntelType,
  updateThreatIntelType,
  viewThreatIntelType,
  addThreatIntelOrigin,
  deleteThreatIntelOrigin,
  updateThreatIntelOrigin,
  viewThreatIntelOrigin,
  addThreatIntelFact,
  traverseThreatIntelFact,
  viewThreatIntelFact,
  addThreatIntelFactComment,
  viewThreatIntelFactComment,
  grantThreatIntelFactAccess,
  viewThreatIntelFactAccess,
  unlimitedThreatIntelSearch;

  @Override
  public String getName() {
    return this.name();
  }
}
