package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.GetFactByIdRequest;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
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
    return TiRequestContext.get().getFactConverter().apply(sanitizeInReferenceTo(entity));
  }

  private FactEntity sanitizeInReferenceTo(FactEntity entity) {
    if (entity.getInReferenceToID() == null) return entity;

    FactEntity inReferenceTo = TiRequestContext.get().getFactManager().getFact(entity.getInReferenceToID());
    if (inReferenceTo == null) return entity;

    try {
      TiSecurityContext.get().checkReadPermission(inReferenceTo);
    } catch (AccessDeniedException | AuthenticationFailedException e) {
      // User doesn't have access to 'inReferenceTo' Fact, thus, it shouldn't be returned as part of the requested Fact.
      // Clone entity first in order to not disturb DAO layer.
      return entity.clone().setInReferenceToID(null);
    }

    return entity;
  }

}
