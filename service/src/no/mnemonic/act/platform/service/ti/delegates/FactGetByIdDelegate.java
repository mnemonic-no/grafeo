package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.GetFactByIdRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;

import javax.inject.Inject;
import java.util.function.Function;

public class FactGetByIdDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final Function<FactEntity, Fact> factConverter;

  @Inject
  public FactGetByIdDelegate(TiSecurityContext securityContext, Function<FactEntity, Fact> factConverter) {
    this.securityContext = securityContext;
    this.factConverter = factConverter;
  }

  public Fact handle(GetFactByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    securityContext.checkPermission(TiFunctionConstants.viewFactObjects);
    FactEntity entity = fetchExistingFact(request.getId());
    securityContext.checkReadPermission(entity);
    return factConverter.apply(entity);
  }
}
