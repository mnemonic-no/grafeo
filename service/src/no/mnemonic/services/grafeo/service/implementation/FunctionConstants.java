package no.mnemonic.services.grafeo.service.implementation;

import no.mnemonic.services.common.auth.model.NamedFunction;

public enum FunctionConstants implements NamedFunction {
  addGrafeoType,
  updateGrafeoType,
  viewGrafeoType,
  addGrafeoOrigin,
  deleteGrafeoOrigin,
  updateGrafeoOrigin,
  viewGrafeoOrigin,
  addGrafeoFact,
  traverseGrafeoFact,
  viewGrafeoFact,
  addGrafeoFactComment,
  viewGrafeoFactComment,
  grantGrafeoFactAccess,
  viewGrafeoFactAccess,
  unlimitedGrafeoSearch;

  @Override
  public String getName() {
    return this.name();
  }
}
