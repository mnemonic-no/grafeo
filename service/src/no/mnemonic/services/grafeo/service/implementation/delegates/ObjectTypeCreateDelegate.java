package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.model.v1.ObjectType;
import no.mnemonic.services.grafeo.api.request.v1.CreateObjectTypeRequest;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.ObjectTypeResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.handlers.ObjectTypeHandler;
import no.mnemonic.services.grafeo.service.implementation.handlers.ValidatorHandler;
import no.mnemonic.services.grafeo.service.validators.Validator;

import javax.inject.Inject;
import java.util.UUID;

import static no.mnemonic.services.grafeo.service.implementation.GrafeoServiceImpl.GLOBAL_NAMESPACE;

public class ObjectTypeCreateDelegate implements Delegate {

  private final GrafeoSecurityContext securityContext;
  private final ObjectManager objectManager;
  private final ObjectTypeResponseConverter objectTypeResponseConverter;
  private final ObjectTypeHandler objectTypeHandler;
  private final ValidatorHandler validatorHandler;

  @Inject
  public ObjectTypeCreateDelegate(GrafeoSecurityContext securityContext,
                                  ObjectManager objectManager,
                                  ObjectTypeResponseConverter objectTypeResponseConverter,
                                  ObjectTypeHandler objectTypeHandler,
                                  ValidatorHandler validatorHandler) {
    this.securityContext = securityContext;
    this.objectManager = objectManager;
    this.objectTypeResponseConverter = objectTypeResponseConverter;
    this.objectTypeHandler = objectTypeHandler;
    this.validatorHandler = validatorHandler;
  }

  public ObjectType handle(CreateObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(FunctionConstants.addThreatIntelType);

    objectTypeHandler.assertObjectTypeNotExists(request.getName());
    validatorHandler.assertValidator(request.getValidator(), request.getValidatorParameter(), Validator.ApplicableType.ObjectType);

    ObjectTypeEntity entity = new ObjectTypeEntity()
            .setId(UUID.randomUUID()) // ID needs to be provided by client.
            .setNamespaceID(GLOBAL_NAMESPACE) // For now everything will just be part of the global namespace.
            .setName(request.getName())
            .setValidator(request.getValidator())
            .setValidatorParameter(request.getValidatorParameter());

    if (request.getIndexOption() == CreateObjectTypeRequest.IndexOption.TimeGlobal) {
      entity.addFlag(ObjectTypeEntity.Flag.TimeGlobalIndex);
    }

    entity = objectManager.saveObjectType(entity);
    return objectTypeResponseConverter.apply(entity);
  }
}
