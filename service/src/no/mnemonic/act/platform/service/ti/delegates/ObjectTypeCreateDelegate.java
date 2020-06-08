package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.CreateObjectTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.ObjectTypeResponseConverter;
import no.mnemonic.act.platform.service.ti.handlers.ObjectTypeHandler;
import no.mnemonic.act.platform.service.ti.handlers.ValidatorHandler;

import javax.inject.Inject;
import java.util.UUID;

import static no.mnemonic.act.platform.service.ti.ThreatIntelligenceServiceImpl.GLOBAL_NAMESPACE;

public class ObjectTypeCreateDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final ObjectManager objectManager;
  private final ObjectTypeResponseConverter objectTypeResponseConverter;
  private final ObjectTypeHandler objectTypeHandler;
  private final ValidatorHandler validatorHandler;

  @Inject
  public ObjectTypeCreateDelegate(TiSecurityContext securityContext,
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
    securityContext.checkPermission(TiFunctionConstants.addThreatIntelType);

    objectTypeHandler.assertObjectTypeNotExists(request.getName());
    validatorHandler.assertValidatorExists(request.getValidator(), request.getValidatorParameter());

    ObjectTypeEntity entity = new ObjectTypeEntity()
            .setId(UUID.randomUUID()) // ID needs to be provided by client.
            .setNamespaceID(GLOBAL_NAMESPACE) // For now everything will just be part of the global namespace.
            .setName(request.getName())
            .setValidator(request.getValidator())
            .setValidatorParameter(request.getValidatorParameter());

    entity = objectManager.saveObjectType(entity);
    return objectTypeResponseConverter.apply(entity);
  }
}
