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

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

import static no.mnemonic.act.platform.service.ti.ThreatIntelligenceServiceImpl.GLOBAL_NAMESPACE;

public class ObjectTypeCreateDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final ObjectManager objectManager;
  private final Function<ObjectTypeEntity, ObjectType> objectTypeConverter;

  @Inject
  public ObjectTypeCreateDelegate(TiSecurityContext securityContext,
                                  ObjectManager objectManager,
                                  Function<ObjectTypeEntity, ObjectType> objectTypeConverter) {
    this.securityContext = securityContext;
    this.objectManager = objectManager;
    this.objectTypeConverter = objectTypeConverter;
  }

  public ObjectType handle(CreateObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.addTypes);

    assertObjectTypeNotExists(request.getName());
    assertValidatorExists(request.getValidator(), request.getValidatorParameter());

    ObjectTypeEntity entity = new ObjectTypeEntity()
            .setId(UUID.randomUUID()) // ID needs to be provided by client.
            .setNamespaceID(GLOBAL_NAMESPACE) // For now everything will just be part of the global namespace.
            .setName(request.getName())
            .setValidator(request.getValidator())
            .setValidatorParameter(request.getValidatorParameter());

    entity = objectManager.saveObjectType(entity);
    return objectTypeConverter.apply(entity);
  }
}
