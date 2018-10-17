package no.mnemonic.act.platform.service.ti;

import no.mnemonic.services.common.auth.model.NamedFunction;

public enum TiFunctionConstants implements NamedFunction {
  addTypes,
  updateTypes,
  viewTypes,
  addFactObjects,
  traverseFactObjects,
  viewFactObjects,
  addFactComments,
  viewFactComments,
  grantFactAccess,
  viewFactAccess;

  @Override
  public String getName() {
    return this.name();
  }
}
