package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.request.v1.UpdateFactTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeHelper;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;
import java.util.Objects;
import java.util.function.Function;

public class FactTypeUpdateDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final FactManager factManager;
  private final FactTypeHelper factTypeHelper;
  private final FactTypeResolver factTypeResolver;
  private final Function<FactTypeEntity, FactType> factTypeConverter;

  @Inject
  public FactTypeUpdateDelegate(TiSecurityContext securityContext,
                                FactManager factManager,
                                FactTypeHelper factTypeHelper,
                                FactTypeResolver factTypeResolver,
                                Function<FactTypeEntity, FactType> factTypeConverter) {
    this.securityContext = securityContext;
    this.factManager = factManager;
    this.factTypeHelper = factTypeHelper;
    this.factTypeResolver = factTypeResolver;
    this.factTypeConverter = factTypeConverter;
  }

  public FactType handle(UpdateFactTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    securityContext.checkPermission(TiFunctionConstants.updateTypes);

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

    entity = factManager.saveFactType(entity);
    return factTypeConverter.apply(entity);
  }
}
