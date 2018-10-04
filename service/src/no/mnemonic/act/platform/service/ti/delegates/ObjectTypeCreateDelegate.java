package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.CreateObjectTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;

import java.util.UUID;

import static no.mnemonic.act.platform.service.ti.ThreatIntelligenceServiceImpl.GLOBAL_NAMESPACE;

public class ObjectTypeCreateDelegate extends AbstractDelegate {

  public static ObjectTypeCreateDelegate create() {
    return new ObjectTypeCreateDelegate();
  }

  public ObjectType handle(CreateObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    SecurityContext.get().checkPermission(TiFunctionConstants.addTypes);

    assertObjectTypeNotExists(request.getName());
    assertValidatorExists(request.getValidator(), request.getValidatorParameter());

    ObjectTypeEntity entity = new ObjectTypeEntity()
            .setId(UUID.randomUUID()) // ID needs to be provided by client.
            .setNamespaceID(GLOBAL_NAMESPACE) // For now everything will just be part of the global namespace.
            .setName(request.getName())
            .setValidator(request.getValidator())
            .setValidatorParameter(request.getValidatorParameter());

    entity = TiRequestContext.get().getObjectManager().saveObjectType(entity);
    return TiRequestContext.get().getObjectTypeConverter().apply(entity);
  }

}
