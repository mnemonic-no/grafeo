package no.mnemonic.services.grafeo.service.ti.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.FactType;
import no.mnemonic.services.grafeo.api.request.v1.GetFactTypeByIdRequest;
import no.mnemonic.services.grafeo.service.ti.TiFunctionConstants;
import no.mnemonic.services.grafeo.service.ti.TiSecurityContext;
import no.mnemonic.services.grafeo.service.ti.converters.response.FactTypeResponseConverter;
import no.mnemonic.services.grafeo.service.ti.resolvers.request.FactTypeRequestResolver;

import javax.inject.Inject;

public class FactTypeGetByIdDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final FactTypeResponseConverter factTypeResponseConverter;
  private final FactTypeRequestResolver factTypeRequestResolver;

  @Inject
  public FactTypeGetByIdDelegate(TiSecurityContext securityContext,
                                 FactTypeResponseConverter factTypeResponseConverter,
                                 FactTypeRequestResolver factTypeRequestResolver) {
    this.securityContext = securityContext;
    this.factTypeResponseConverter = factTypeResponseConverter;
    this.factTypeRequestResolver = factTypeRequestResolver;
  }

  public FactType handle(GetFactTypeByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    securityContext.checkPermission(TiFunctionConstants.viewThreatIntelType);
    return factTypeResponseConverter.apply(factTypeRequestResolver.fetchExistingFactType(request.getId()));
  }
}
