package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.CreateFactRequest;
import no.mnemonic.act.platform.dao.cassandra.exceptions.ImmutableViolationException;
import no.mnemonic.act.platform.entity.cassandra.*;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.helpers.FactStorageHelper;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver;
import no.mnemonic.act.platform.service.ti.helpers.ObjectResolver;
import no.mnemonic.act.platform.service.validators.Validator;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FactCreateDelegate extends AbstractDelegate {

  private final FactTypeResolver factTypeResolver;
  private final ObjectResolver objectResolver;
  private final FactStorageHelper factStorageHelper;

  private FactCreateDelegate(FactTypeResolver factTypeResolver, ObjectResolver objectResolver, FactStorageHelper factStorageHelper) {
    this.factTypeResolver = factTypeResolver;
    this.objectResolver = objectResolver;
    this.factStorageHelper = factStorageHelper;
  }

  public Fact handle(CreateFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    // Verify that user is allowed to add Facts for the requested organization.
    TiSecurityContext.get().checkPermission(TiFunctionConstants.addFactObjects, resolveOrganization(request.getOrganization()));

    // Validate that requested Fact matches its FactType.
    FactTypeEntity type = factTypeResolver.resolveFactType(request.getType());
    validateFactValue(type, request.getValue());
    validateFactObjectBindings(type, request.getBindings());

    FactEntity fact = resolveExistingFact(request, type);
    if (fact != null) {
      // Refresh an existing Fact.
      fact = TiRequestContext.get().getFactManager().refreshFact(fact.getId());
      factStorageHelper.saveAdditionalAclForFact(fact, request.getAcl());
    } else {
      // Or create a new Fact.
      fact = saveFact(request, type);
      factStorageHelper.saveInitialAclForNewFact(fact, request.getAcl());
    }

    // Always add provided comment.
    factStorageHelper.saveCommentForFact(fact, request.getComment());

    return TiRequestContext.get().getFactConverter().apply(fact);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private FactTypeResolver factTypeResolver;
    private ObjectResolver objectResolver;
    private FactStorageHelper factStorageHelper;

    private Builder() {
    }

    public FactCreateDelegate build() {
      ObjectUtils.notNull(factTypeResolver, "Cannot instantiate FactCreateDelegate without 'factTypeResolver'.");
      ObjectUtils.notNull(objectResolver, "Cannot instantiate FactCreateDelegate without 'objectResolver'.");
      ObjectUtils.notNull(factStorageHelper, "Cannot instantiate FactCreateDelegate without 'factStorageHelper'.");
      return new FactCreateDelegate(factTypeResolver, objectResolver, factStorageHelper);
    }

    public Builder setFactTypeResolver(FactTypeResolver factTypeResolver) {
      this.factTypeResolver = factTypeResolver;
      return this;
    }

    public Builder setObjectResolver(ObjectResolver objectResolver) {
      this.objectResolver = objectResolver;
      return this;
    }

    public Builder setFactStorageHelper(FactStorageHelper factStorageHelper) {
      this.factStorageHelper = factStorageHelper;
      return this;
    }
  }

  private void validateFactValue(FactTypeEntity type, String value) throws InvalidArgumentException {
    Validator validator = TiRequestContext.get().getValidatorFactory().get(type.getValidator(), type.getValidatorParameter());
    if (!validator.validate(value)) {
      throw new InvalidArgumentException()
              .addValidationError("Fact did not pass validation against FactType.", "fact.not.valid", "value", value);
    }
  }

  private void validateFactObjectBindings(FactTypeEntity type, List<CreateFactRequest.FactObjectBinding> requestedBindings)
          throws InvalidArgumentException {
    InvalidArgumentException ex = new InvalidArgumentException();

    for (int i = 0; i < requestedBindings.size(); i++) {
      CreateFactRequest.FactObjectBinding requested = requestedBindings.get(i);
      ObjectEntity object = objectResolver.resolveObject(requested.getObjectID(), requested.getObjectType(), requested.getObjectValue());

      // Check requested binding against all definitions.
      boolean valid = type.getRelevantObjectBindings()
              .stream()
              .anyMatch(b -> b.getObjectTypeID() == object.getTypeID() && Objects.equals(b.getDirection().name(), requested.getDirection().name()));
      if (!valid) {
        // Requested binding is invalid, add to exception and continue to validate other bindings.
        ex.addValidationError("Requested binding between Fact and Object is not allowed.", "invalid.fact.object.binding", "bindings." + i, requested.toString());
      }
    }

    if (!CollectionUtils.isEmpty(ex.getValidationErrors())) {
      throw ex;
    }
  }

  private FactEntity resolveExistingFact(CreateFactRequest request, FactTypeEntity type) {
    // Skip confidenceLevel for now as it's currently not provided in the request.
    FactEntity existingFact = TiRequestContext.get().getFactManager().fetchFactsByValue(request.getValue())
            .stream()
            .filter(f -> f.getTypeID().equals(type.getId()))
            .filter(f -> f.getSourceID().equals(resolveSource(request.getSource())))
            .filter(f -> f.getOrganizationID().equals(resolveOrganization(request.getOrganization())))
            .filter(f -> f.getAccessMode().name().equals(request.getAccessMode().name()))
            .findFirst()
            .orElse(null);

    try {
      // Make sure that user has access to an existing Fact ...
      TiSecurityContext.get().checkReadPermission(existingFact);
    } catch (AccessDeniedException | AuthenticationFailedException ignored) {
      // ... if not a new Fact needs to be created.
      return null;
    }

    return existingFact;
  }

  private FactEntity saveFact(CreateFactRequest request, FactTypeEntity type)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())  // Need to provide client-generated ID.
            .setTypeID(type.getId())
            .setValue(request.getValue())
            .setAccessMode(AccessMode.valueOf(request.getAccessMode().name()))
            .setInReferenceToID(resolveInReferenceTo(request.getInReferenceTo()))
            .setOrganizationID(resolveOrganization(request.getOrganization()))
            .setSourceID(resolveSource(request.getSource()))
            .setBindings(resolveFactObjectBindings(request.getBindings()))
            .setTimestamp(System.currentTimeMillis())
            .setLastSeenTimestamp(System.currentTimeMillis());

    try {
      fact = TiRequestContext.get().getFactManager().saveFact(fact);
      // Save all bindings between Objects and the created Facts.
      for (FactEntity.FactObjectBinding binding : fact.getBindings()) {
        ObjectFactBindingEntity entity = new ObjectFactBindingEntity()
                .setObjectID(binding.getObjectID())
                .setFactID(fact.getId())
                .setDirection(binding.getDirection());
        TiRequestContext.get().getObjectManager().saveObjectFactBinding(entity);
      }
    } catch (ImmutableViolationException ex) {
      // This should never happen because a new entity with an own UUID was created.
      // Update 'lastSeenTimestamp' is a separate operation.
      throw new RuntimeException(ex);
    }

    return fact;
  }

  private UUID resolveInReferenceTo(UUID inReferenceToID)
          throws InvalidArgumentException, AccessDeniedException, AuthenticationFailedException {
    if (inReferenceToID != null) {
      FactEntity inReferenceTo = TiRequestContext.get().getFactManager().getFact(inReferenceToID);
      if (inReferenceTo == null) {
        // Referenced Fact must exist.
        throw new InvalidArgumentException()
                .addValidationError("Referenced Fact does not exist.", "referenced.fact.not.exist", "inReferenceTo", inReferenceToID.toString());
      }
      // User must have access to referenced Fact.
      TiSecurityContext.get().checkReadPermission(inReferenceTo);
    }
    // Everything ok, just return ID.
    return inReferenceToID;
  }

  private List<FactEntity.FactObjectBinding> resolveFactObjectBindings(List<CreateFactRequest.FactObjectBinding> requestedBindings)
          throws InvalidArgumentException {
    List<FactEntity.FactObjectBinding> entityBindings = new ArrayList<>();

    for (CreateFactRequest.FactObjectBinding requested : requestedBindings) {
      ObjectEntity object = objectResolver.resolveObject(requested.getObjectID(), requested.getObjectType(), requested.getObjectValue());
      FactEntity.FactObjectBinding entity = new FactEntity.FactObjectBinding()
              .setObjectID(object.getId())
              .setDirection(Direction.valueOf(requested.getDirection().name()));
      entityBindings.add(entity);
    }

    return entityBindings;
  }

}
