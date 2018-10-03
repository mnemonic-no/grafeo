package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.request.v1.CreateFactTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;

import java.util.UUID;

import static no.mnemonic.act.platform.service.ti.ThreatIntelligenceServiceImpl.GLOBAL_NAMESPACE;

public class FactTypeCreateDelegate extends AbstractDelegate {

  public static FactTypeCreateDelegate create() {
    return new FactTypeCreateDelegate();
  }

  public FactType handle(CreateFactTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    SecurityContext.get().checkPermission(TiFunctionConstants.addTypes);

    assertFactTypeNotExists(request.getName());
    assertObjectTypesToBindExist(request.getRelevantObjectBindings(), "relevantObjectBindings");
    assertEntityHandlerExists(request.getEntityHandler(), request.getEntityHandlerParameter());
    assertValidatorExists(request.getValidator(), request.getValidatorParameter());

    FactTypeEntity entity = new FactTypeEntity()
            .setId(UUID.randomUUID()) // ID needs to be provided by client.
            .setNamespaceID(GLOBAL_NAMESPACE) // For now everything will just be part of the global namespace.
            .setName(request.getName())
            .setEntityHandler(request.getEntityHandler())
            .setEntityHandlerParameter(request.getEntityHandlerParameter())
            .setValidator(request.getValidator())
            .setValidatorParameter(request.getValidatorParameter())
            .setRelevantObjectBindings(convertFactObjectBindingDefinitions(request.getRelevantObjectBindings()));

    entity = TiRequestContext.get().getFactManager().saveFactType(entity);
    return TiRequestContext.get().getFactTypeConverter().apply(entity);
  }

}
