package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.Fact;
import no.mnemonic.services.grafeo.api.request.v1.GetFactByIdRequest;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.FactResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactRequestResolver;

import jakarta.inject.Inject;

public class FactGetByIdDelegate implements Delegate {

  private final GrafeoSecurityContext securityContext;
  private final FactRequestResolver factRequestResolver;
  private final FactResponseConverter factResponseConverter;

  @Inject
  public FactGetByIdDelegate(GrafeoSecurityContext securityContext,
                             FactRequestResolver factRequestResolver,
                             FactResponseConverter factResponseConverter) {
    this.securityContext = securityContext;
    this.factRequestResolver = factRequestResolver;
    this.factResponseConverter = factResponseConverter;
  }

  public Fact handle(GetFactByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    securityContext.checkPermission(FunctionConstants.viewGrafeoFact);
    FactRecord record = factRequestResolver.resolveFact(request.getId());
    securityContext.checkReadPermission(record);
    return factResponseConverter.apply(record);
  }
}
