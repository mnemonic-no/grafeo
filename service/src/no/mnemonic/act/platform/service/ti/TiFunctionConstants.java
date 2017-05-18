package no.mnemonic.act.platform.service.ti;

import no.mnemonic.services.common.auth.model.NamedFunction;

public enum TiFunctionConstants implements NamedFunction {
  addTypes,
  updateTypes,
  viewTypes,
  addFactObjects,
  grantFactAccess,
  viewFactObjects;

  @Override
  public String getName() {
    return this.name();
  }
}
