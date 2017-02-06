package no.mnemonic.act.platform.service.ti;

import no.mnemonic.act.platform.service.contexts.SecurityContext;

public enum TiFunctionConstants implements SecurityContext.NamedFunction {
  addTypes,
  updateTypes,
  viewTypes;

  @Override
  public String getName() {
    return this.name();
  }
}
