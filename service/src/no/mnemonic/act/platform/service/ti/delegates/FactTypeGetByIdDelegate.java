package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.request.v1.GetFactTypeByIdRequest;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.FactTypeConverter;
import no.mnemonic.act.platform.service.ti.resolvers.FactTypeResolver;

import javax.inject.Inject;

public class FactTypeGetByIdDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final FactTypeConverter factTypeConverter;
  private final FactTypeResolver factTypeResolver;

  @Inject
  public FactTypeGetByIdDelegate(TiSecurityContext securityContext,
                                 FactTypeConverter factTypeConverter,
                                 FactTypeResolver factTypeResolver) {
    this.securityContext = securityContext;
    this.factTypeConverter = factTypeConverter;
    this.factTypeResolver = factTypeResolver;
  }

  public FactType handle(GetFactTypeByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    securityContext.checkPermission(TiFunctionConstants.viewTypes);
    return factTypeConverter.apply(factTypeResolver.fetchExistingFactType(request.getId()));
  }
}
