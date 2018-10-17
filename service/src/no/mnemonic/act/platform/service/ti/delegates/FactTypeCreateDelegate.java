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
import no.mnemonic.act.platform.service.ti.helpers.FactTypeHelper;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import java.util.UUID;

import static no.mnemonic.act.platform.service.ti.ThreatIntelligenceServiceImpl.GLOBAL_NAMESPACE;

public class FactTypeCreateDelegate extends AbstractDelegate {

  private final FactTypeHelper factTypeHelper;

  private FactTypeCreateDelegate(FactTypeHelper factTypeHelper) {
    this.factTypeHelper = factTypeHelper;
  }

  public FactType handle(CreateFactTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    SecurityContext.get().checkPermission(TiFunctionConstants.addTypes);

    // A FactType can either link Objects or Facts not both, i.e. exactly one of 'relevantObjectBindings' or 'relevantFactBindings' must be set
    if (CollectionUtils.isEmpty(request.getRelevantObjectBindings()) && CollectionUtils.isEmpty(request.getRelevantFactBindings())) {
      throw new InvalidArgumentException()
              .addValidationError("One of 'relevantObjectBindings' or 'relevantFactBindings' must be set.",
                      "invalid.fact.type.definition", "relevantObjectBindings", "NULL")
              .addValidationError("One of 'relevantObjectBindings' or 'relevantFactBindings' must be set.",
                      "invalid.fact.type.definition", "relevantFactBindings", "NULL");
    }
    if (!CollectionUtils.isEmpty(request.getRelevantObjectBindings()) && !CollectionUtils.isEmpty(request.getRelevantFactBindings())) {
      throw new InvalidArgumentException()
              .addValidationError("Not allowed to set both 'relevantObjectBindings' and 'relevantFactBindings'.",
                      "invalid.fact.type.definition", "relevantObjectBindings", null)
              .addValidationError("Not allowed to set both 'relevantObjectBindings' and 'relevantFactBindings'.",
                      "invalid.fact.type.definition", "relevantFactBindings", null);
    }

    factTypeHelper.assertFactTypeNotExists(request.getName());
    factTypeHelper.assertObjectTypesToBindExist(request.getRelevantObjectBindings(), "relevantObjectBindings");
    factTypeHelper.assertFactTypesToBindExist(request.getRelevantFactBindings(), "relevantFactBindings");
    assertValidatorExists(request.getValidator(), request.getValidatorParameter());

    FactTypeEntity entity = new FactTypeEntity()
            .setId(UUID.randomUUID()) // ID needs to be provided by client.
            .setNamespaceID(GLOBAL_NAMESPACE) // For now everything will just be part of the global namespace.
            .setName(request.getName())
            .setValidator(request.getValidator())
            .setValidatorParameter(request.getValidatorParameter())
            .setRelevantObjectBindings(factTypeHelper.convertFactObjectBindingDefinitions(request.getRelevantObjectBindings()))
            .setRelevantFactBindings(factTypeHelper.convertMetaFactBindingDefinitions(request.getRelevantFactBindings()));

    entity = TiRequestContext.get().getFactManager().saveFactType(entity);
    return TiRequestContext.get().getFactTypeConverter().apply(entity);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private FactTypeHelper factTypeHelper;

    private Builder() {
    }

    public FactTypeCreateDelegate build() {
      ObjectUtils.notNull(factTypeHelper, "Cannot instantiate FactTypeCreateDelegate without 'factTypeHelper'.");
      return new FactTypeCreateDelegate(factTypeHelper);
    }

    public Builder setFactTypeHelper(FactTypeHelper factTypeHelper) {
      this.factTypeHelper = factTypeHelper;
      return this;
    }
  }
}
