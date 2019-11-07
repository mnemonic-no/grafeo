package no.mnemonic.act.platform.service.ti;

import no.mnemonic.services.common.auth.model.NamedFunction;

public enum TiFunctionConstants implements NamedFunction {
  addTypes,
  updateTypes,
  viewTypes,
  addOrigins,
  deleteOrigins,
  updateOrigins,
  viewOrigins,
  addFactObjects,
  traverseFactObjects,
  viewFactObjects,
  addFactComments,
  viewFactComments,
  grantFactAccess,
  viewFactAccess,
  unlimitedSearch;

  @Override
  public String getName() {
    return this.name();
  }
}
