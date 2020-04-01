package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.request.v1.GetFactTypeByIdRequest;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.FactTypeResponseConverter;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactTypeRequestResolver;

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
    securityContext.checkPermission(TiFunctionConstants.viewTypes);
    return factTypeResponseConverter.apply(factTypeRequestResolver.fetchExistingFactType(request.getId()));
  }
}
