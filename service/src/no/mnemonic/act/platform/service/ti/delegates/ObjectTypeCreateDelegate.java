package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.CreateObjectTypeRequest;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;

import java.util.UUID;

import static no.mnemonic.act.platform.service.ti.ThreatIntelligenceServiceImpl.GLOBAL_NAMESPACE;

public class ObjectTypeCreateDelegate {

  public static ObjectTypeCreateDelegate create() {
    return new ObjectTypeCreateDelegate();
  }

  public ObjectType handle(CreateObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    SecurityContext.get().checkPermission(TiFunctionConstants.addTypes);

    assertObjectTypeNotExists(request);
    assertEntityHandlerExists(request);
    assertValidatorExists(request);

    ObjectTypeEntity entity = new ObjectTypeEntity()
            .setId(UUID.randomUUID()) // ID needs to be provided by client.
            .setNamespaceID(GLOBAL_NAMESPACE) // For now everything will just be part of the global namespace.
            .setName(request.getName())
            .setEntityHandler(request.getEntityHandler())
            .setEntityHandlerParameter(request.getEntityHandlerParameter())
            .setValidator(request.getValidator())
            .setValidatorParameter(request.getValidatorParameter());

    entity = TiRequestContext.get().getObjectManager().saveObjectType(entity);
    return TiRequestContext.get().getObjectTypeConverter().apply(entity);
  }

  private void assertObjectTypeNotExists(CreateObjectTypeRequest request) throws InvalidArgumentException {
    if (TiRequestContext.get().getObjectManager().getObjectType(request.getName()) != null) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("ObjectType with name = %s already exists.", request.getName()),
                      "object.type.exist", "name", request.getName());
    }
  }

  private void assertEntityHandlerExists(CreateObjectTypeRequest request) throws InvalidArgumentException {
    try {
      TiRequestContext.get().getEntityHandlerFactory().get(request.getEntityHandler(), request.getEntityHandlerParameter());
    } catch (IllegalArgumentException ex) {
      // An IllegalArgumentException will be thrown if an EntityHandler cannot be found.
      throw new InvalidArgumentException()
              .addValidationError(ex.getMessage(), "entity.handler.not.exist", "entityHandler", request.getEntityHandler());
    }
  }

  private void assertValidatorExists(CreateObjectTypeRequest request) throws InvalidArgumentException {
    try {
      TiRequestContext.get().getValidatorFactory().get(request.getValidator(), request.getValidatorParameter());
    } catch (IllegalArgumentException ex) {
      // An IllegalArgumentException will be thrown if a Validator cannot be found.
      throw new InvalidArgumentException()
              .addValidationError(ex.getMessage(), "validator.not.exist", "validator", request.getValidator());
    }
  }

}
