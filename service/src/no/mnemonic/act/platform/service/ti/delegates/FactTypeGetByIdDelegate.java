package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.request.v1.GetFactTypeByIdRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;

import javax.inject.Inject;
import java.util.function.Function;

public class FactTypeGetByIdDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final Function<FactTypeEntity, FactType> factTypeConverter;

  @Inject
  public FactTypeGetByIdDelegate(TiSecurityContext securityContext,
                                 Function<FactTypeEntity, FactType> factTypeConverter) {
    this.securityContext = securityContext;
    this.factTypeConverter = factTypeConverter;
  }

  public FactType handle(GetFactTypeByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    securityContext.checkPermission(TiFunctionConstants.viewTypes);
    return factTypeConverter.apply(fetchExistingFactType(request.getId()));
  }
}
