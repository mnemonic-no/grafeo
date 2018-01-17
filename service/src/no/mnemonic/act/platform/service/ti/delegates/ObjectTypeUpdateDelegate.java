package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.UpdateObjectTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.commons.utilities.StringUtils;

public class ObjectTypeUpdateDelegate extends AbstractDelegate {

  public static ObjectTypeUpdateDelegate create() {
    return new ObjectTypeUpdateDelegate();
  }

  public ObjectType handle(UpdateObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    SecurityContext.get().checkPermission(TiFunctionConstants.updateTypes);

    ObjectTypeEntity entity = fetchExistingObjectType(request.getId());

    if (!StringUtils.isBlank(request.getName())) {
      assertObjectTypeNotExists(request.getName());
      entity.setName(request.getName());
    }

    entity = TiRequestContext.get().getObjectManager().saveObjectType(entity);
    return TiRequestContext.get().getObjectTypeConverter().apply(entity);
  }

}
