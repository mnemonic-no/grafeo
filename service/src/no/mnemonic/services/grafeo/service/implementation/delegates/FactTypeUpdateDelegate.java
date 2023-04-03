package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.FactType;
import no.mnemonic.services.grafeo.api.request.v1.UpdateFactTypeRequest;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.FactTypeResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.helpers.FactTypeHelper;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactTypeRequestResolver;

import javax.inject.Inject;
import java.util.Objects;

public class FactTypeUpdateDelegate implements Delegate {

  private final GrafeoSecurityContext securityContext;
  private final FactManager factManager;
  private final FactTypeHelper factTypeHelper;
  private final FactTypeRequestResolver factTypeRequestResolver;
  private final FactTypeResponseConverter factTypeResponseConverter;

  @Inject
  public FactTypeUpdateDelegate(GrafeoSecurityContext securityContext,
                                FactManager factManager,
                                FactTypeHelper factTypeHelper,
                                FactTypeRequestResolver factTypeRequestResolver,
                                FactTypeResponseConverter factTypeResponseConverter) {
    this.securityContext = securityContext;
    this.factManager = factManager;
    this.factTypeHelper = factTypeHelper;
    this.factTypeRequestResolver = factTypeRequestResolver;
    this.factTypeResponseConverter = factTypeResponseConverter;
  }

  public FactType handle(UpdateFactTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    securityContext.checkPermission(FunctionConstants.updateThreatIntelType);

    FactTypeEntity entity = factTypeRequestResolver.fetchExistingFactType(request.getId());
    if (Objects.equals(entity.getId(), factTypeRequestResolver.resolveRetractionFactType().getId())) {
      throw new AccessDeniedException("Not allowed to update the system-defined Retraction FactType.");
    }

    if (!StringUtils.isBlank(request.getName())) {
      factTypeHelper.assertFactTypeNotExists(request.getName());
      entity.setName(request.getName());
    }

    if (request.getDefaultConfidence() != null) {
      entity.setDefaultConfidence(request.getDefaultConfidence());
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
    return factTypeResponseConverter.apply(entity);
  }
}
