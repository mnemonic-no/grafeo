package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.GetFactByIdRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;

public class FactGetByIdDelegate extends AbstractDelegate {

  public static FactGetByIdDelegate create() {
    return new FactGetByIdDelegate();
  }

  public Fact handle(GetFactByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    TiSecurityContext.get().checkPermission(TiFunctionConstants.viewFactObjects);
    FactEntity entity = fetchExistingFact(request.getId());
    TiSecurityContext.get().checkReadPermission(entity);
    return TiRequestContext.get().getFactConverter().apply(entity);
  }

}
