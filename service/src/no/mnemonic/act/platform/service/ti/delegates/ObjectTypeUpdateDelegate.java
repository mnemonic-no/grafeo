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
import no.mnemonic.act.platform.service.ti.converters.response.ObjectTypeResponseConverter;
import no.mnemonic.act.platform.service.ti.handlers.ObjectTypeHandler;
import no.mnemonic.act.platform.service.ti.resolvers.request.ObjectTypeRequestResolver;
import no.mnemonic.commons.utilities.StringUtils;

import javax.inject.Inject;

public class ObjectTypeUpdateDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final ObjectManager objectManager;
  private final ObjectTypeResponseConverter objectTypeResponseConverter;
  private final ObjectTypeRequestResolver objectTypeRequestResolver;
  private final ObjectTypeHandler objectTypeHandler;

  @Inject
  public ObjectTypeUpdateDelegate(TiSecurityContext securityContext,
                                  ObjectManager objectManager,
                                  ObjectTypeResponseConverter objectTypeResponseConverter,
                                  ObjectTypeRequestResolver objectTypeRequestResolver,
                                  ObjectTypeHandler objectTypeHandler) {
    this.securityContext = securityContext;
    this.objectManager = objectManager;
    this.objectTypeResponseConverter = objectTypeResponseConverter;
    this.objectTypeRequestResolver = objectTypeRequestResolver;
    this.objectTypeHandler = objectTypeHandler;
  }

  public ObjectType handle(UpdateObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    securityContext.checkPermission(TiFunctionConstants.updateThreatIntelType);

    ObjectTypeEntity entity = objectTypeRequestResolver.fetchExistingObjectType(request.getId());

    if (!StringUtils.isBlank(request.getName())) {
      objectTypeHandler.assertObjectTypeNotExists(request.getName());
      entity.setName(request.getName());
    }

    entity = objectManager.saveObjectType(entity);
    return objectTypeResponseConverter.apply(entity);
  }
}
