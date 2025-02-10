package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.FactType;
import no.mnemonic.services.grafeo.api.request.v1.GetFactTypeByIdRequest;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.FactTypeResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactTypeRequestResolver;

import jakarta.inject.Inject;

public class FactTypeGetByIdDelegate implements Delegate {

  private final GrafeoSecurityContext securityContext;
  private final FactTypeResponseConverter factTypeResponseConverter;
  private final FactTypeRequestResolver factTypeRequestResolver;

  @Inject
  public FactTypeGetByIdDelegate(GrafeoSecurityContext securityContext,
                                 FactTypeResponseConverter factTypeResponseConverter,
                                 FactTypeRequestResolver factTypeRequestResolver) {
    this.securityContext = securityContext;
    this.factTypeResponseConverter = factTypeResponseConverter;
    this.factTypeRequestResolver = factTypeRequestResolver;
  }

  public FactType handle(GetFactTypeByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    securityContext.checkPermission(FunctionConstants.viewGrafeoType);
    return factTypeResponseConverter.apply(factTypeRequestResolver.fetchExistingFactType(request.getId()));
  }
}
