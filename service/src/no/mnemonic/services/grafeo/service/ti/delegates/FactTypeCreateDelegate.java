package no.mnemonic.services.grafeo.service.ti.delegates;

import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.model.v1.FactType;
import no.mnemonic.services.grafeo.api.request.v1.CreateFactTypeRequest;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.service.ti.TiFunctionConstants;
import no.mnemonic.services.grafeo.service.ti.TiSecurityContext;
import no.mnemonic.services.grafeo.service.ti.converters.response.FactTypeResponseConverter;
import no.mnemonic.services.grafeo.service.ti.handlers.ValidatorHandler;
import no.mnemonic.services.grafeo.service.ti.helpers.FactTypeHelper;
import no.mnemonic.services.grafeo.service.validators.Validator;

import javax.inject.Inject;
import java.util.UUID;

import static no.mnemonic.services.grafeo.service.ti.ThreatIntelligenceServiceImpl.GLOBAL_NAMESPACE;

public class FactTypeCreateDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final FactManager factManager;
  private final FactTypeHelper factTypeHelper;
  private final FactTypeResponseConverter factTypeResponseConverter;
  private final ValidatorHandler validatorHandler;

  @Inject
  public FactTypeCreateDelegate(TiSecurityContext securityContext,
                                FactManager factManager,
                                FactTypeHelper factTypeHelper,
                                FactTypeResponseConverter factTypeResponseConverter,
                                ValidatorHandler validatorHandler) {
    this.securityContext = securityContext;
    this.factManager = factManager;
    this.factTypeHelper = factTypeHelper;
    this.factTypeResponseConverter = factTypeResponseConverter;
    this.validatorHandler = validatorHandler;
  }

  public FactType handle(CreateFactTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.addThreatIntelType);

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
    validatorHandler.assertValidator(request.getValidator(), request.getValidatorParameter(), Validator.ApplicableType.FactType);

    FactTypeEntity entity = new FactTypeEntity()
            .setId(UUID.randomUUID()) // ID needs to be provided by client.
            .setNamespaceID(GLOBAL_NAMESPACE) // For now everything will just be part of the global namespace.
            .setName(request.getName())
            .setDefaultConfidence(request.getDefaultConfidence())
            .setValidator(request.getValidator())
            .setValidatorParameter(request.getValidatorParameter())
            .setRelevantObjectBindings(factTypeHelper.convertFactObjectBindingDefinitions(request.getRelevantObjectBindings()))
            .setRelevantFactBindings(factTypeHelper.convertMetaFactBindingDefinitions(request.getRelevantFactBindings()));

    entity = factManager.saveFactType(entity);
    return factTypeResponseConverter.apply(entity);
  }
}
