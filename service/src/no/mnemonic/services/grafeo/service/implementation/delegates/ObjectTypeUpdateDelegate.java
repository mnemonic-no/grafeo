package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.ObjectType;
import no.mnemonic.services.grafeo.api.request.v1.UpdateObjectTypeRequest;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.ObjectTypeResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.handlers.ObjectTypeHandler;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.ObjectTypeRequestResolver;

import javax.inject.Inject;

public class ObjectTypeUpdateDelegate implements Delegate {

  private final GrafeoSecurityContext securityContext;
  private final ObjectManager objectManager;
  private final ObjectTypeResponseConverter objectTypeResponseConverter;
  private final ObjectTypeRequestResolver objectTypeRequestResolver;
  private final ObjectTypeHandler objectTypeHandler;

  @Inject
  public ObjectTypeUpdateDelegate(GrafeoSecurityContext securityContext,
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
    securityContext.checkPermission(FunctionConstants.updateThreatIntelType);

    ObjectTypeEntity entity = objectTypeRequestResolver.fetchExistingObjectType(request.getId());

    if (!StringUtils.isBlank(request.getName())) {
      objectTypeHandler.assertObjectTypeNotExists(request.getName());
      entity.setName(request.getName());
    }

    entity = objectManager.saveObjectType(entity);
    return objectTypeResponseConverter.apply(entity);
  }
}
