package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.UpdateObjectTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.ObjectTypeConverter;
import no.mnemonic.commons.utilities.StringUtils;

import javax.inject.Inject;

public class ObjectTypeUpdateDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final ObjectManager objectManager;
  private final ObjectTypeConverter objectTypeConverter;

  @Inject
  public ObjectTypeUpdateDelegate(TiSecurityContext securityContext,
                                  ObjectManager objectManager,
                                  ObjectTypeConverter objectTypeConverter) {
    this.securityContext = securityContext;
    this.objectManager = objectManager;
    this.objectTypeConverter = objectTypeConverter;
  }

  public ObjectType handle(UpdateObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    securityContext.checkPermission(TiFunctionConstants.updateTypes);

    ObjectTypeEntity entity = fetchExistingObjectType(request.getId());

    if (!StringUtils.isBlank(request.getName())) {
      assertObjectTypeNotExists(request.getName());
      entity.setName(request.getName());
    }

    entity = objectManager.saveObjectType(entity);
    return objectTypeConverter.apply(entity);
  }
}
