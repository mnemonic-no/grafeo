package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.request.v1.UpdateFactTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeHelper;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Objects;

public class FactTypeUpdateDelegate extends AbstractDelegate {

  private final FactTypeHelper factTypeHelper;
  private final FactTypeResolver factTypeResolver;

  private FactTypeUpdateDelegate(FactTypeHelper factTypeHelper, FactTypeResolver factTypeResolver) {
    this.factTypeHelper = factTypeHelper;
    this.factTypeResolver = factTypeResolver;
  }

  public FactType handle(UpdateFactTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    SecurityContext.get().checkPermission(TiFunctionConstants.updateTypes);

    FactTypeEntity entity = fetchExistingFactType(request.getId());
    if (Objects.equals(entity.getId(), factTypeResolver.resolveRetractionFactType().getId())) {
      throw new AccessDeniedException("Not allowed to update the system-defined Retraction FactType.");
    }

    if (!StringUtils.isBlank(request.getName())) {
      factTypeHelper.assertFactTypeNotExists(request.getName());
      entity.setName(request.getName());
    }

    if (!CollectionUtils.isEmpty(request.getAddObjectBindings())) {
      if (!CollectionUtils.isEmpty(entity.getRelevantFactBindings())) {
        throw new InvalidArgumentException()
                .addValidationError("Not allowed to add Object bindings to a FactType which has Fact bindings set.",
                        "invalid.fact.type.definition", "addObjectBindings", null);
      }

      factTypeHelper.assertObjectTypesToBindExist(request.getAddObjectBindings(), "addObjectBindings");
      entity.setRelevantObjectBindings(SetUtils.union(entity.getRelevantObjectBindings(), factTypeHelper.convertFactObjectBindingDefinitions(request.getAddObjectBindings())));
    }

    if (!CollectionUtils.isEmpty(request.getAddFactBindings())) {
      if (!CollectionUtils.isEmpty(entity.getRelevantObjectBindings())) {
        throw new InvalidArgumentException()
                .addValidationError("Not allowed to add Fact bindings to a FactType which has Object bindings set.",
                        "invalid.fact.type.definition", "addFactBindings", null);
      }

      factTypeHelper.assertFactTypesToBindExist(request.getAddFactBindings(), "addFactBindings");
      entity.setRelevantFactBindings(SetUtils.union(entity.getRelevantFactBindings(), factTypeHelper.convertMetaFactBindingDefinitions(request.getAddFactBindings())));
    }

    entity = TiRequestContext.get().getFactManager().saveFactType(entity);
    return TiRequestContext.get().getFactTypeConverter().apply(entity);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private FactTypeHelper factTypeHelper;
    private FactTypeResolver factTypeResolver;

    private Builder() {
    }

    public FactTypeUpdateDelegate build() {
      ObjectUtils.notNull(factTypeHelper, "Cannot instantiate FactTypeUpdateDelegate without 'factTypeHelper'.");
      ObjectUtils.notNull(factTypeResolver, "Cannot instantiate FactTypeUpdateDelegate without 'factTypeResolver'.");
      return new FactTypeUpdateDelegate(factTypeHelper, factTypeResolver);
    }

    public Builder setFactTypeHelper(FactTypeHelper factTypeHelper) {
      this.factTypeHelper = factTypeHelper;
      return this;
    }

    public Builder setFactTypeResolver(FactTypeResolver factTypeResolver) {
      this.factTypeResolver = factTypeResolver;
      return this;
    }
  }
}
