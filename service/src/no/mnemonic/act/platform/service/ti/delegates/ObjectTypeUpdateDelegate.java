package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.UpdateObjectTypeRequest;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.commons.utilities.StringUtils;

public class ObjectTypeUpdateDelegate {

  public static ObjectTypeUpdateDelegate create() {
    return new ObjectTypeUpdateDelegate();
  }

  public ObjectType handle(UpdateObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    SecurityContext.get().checkPermission(TiFunctionConstants.updateTypes);

    ObjectTypeEntity entity = fetchExistingObjectType(request);

    if (!StringUtils.isBlank(request.getName())) {
      assertObjectTypeNotExists(request);
      entity.setName(request.getName());
    }

    entity = TiRequestContext.get().getObjectManager().saveObjectType(entity);
    return TiRequestContext.get().getObjectTypeConverter().apply(entity);
  }

  private ObjectTypeEntity fetchExistingObjectType(UpdateObjectTypeRequest request) throws ObjectNotFoundException {
    ObjectTypeEntity entity = TiRequestContext.get().getObjectManager().getObjectType(request.getId());
    if (entity == null) {
      throw new ObjectNotFoundException(String.format("ObjectType with id = %s does not exist.", request.getId()));
    }
    return entity;
  }

  private void assertObjectTypeNotExists(UpdateObjectTypeRequest request) throws InvalidArgumentException {
    if (TiRequestContext.get().getObjectManager().getObjectType(request.getName()) != null) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("ObjectType with name = %s already exists.", request.getName()),
                      "object.type.exist", "name", request.getName());
    }
  }

}
