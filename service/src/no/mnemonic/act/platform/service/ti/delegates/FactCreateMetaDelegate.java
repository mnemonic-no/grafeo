package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.request.v1.CreateMetaFactRequest;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.FactCreateHandler;
import no.mnemonic.act.platform.service.ti.resolvers.FactResolver;
import no.mnemonic.act.platform.service.ti.resolvers.FactTypeResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.Objects;
import java.util.UUID;

public class FactCreateMetaDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final FactTypeResolver factTypeResolver;
  private final FactResolver factResolver;
  private final FactCreateHandler factCreateHandler;

  private FactTypeEntity requestedFactType;
  private OriginEntity requestedOrigin;
  private Organization requestedOrganization;

  @Inject
  public FactCreateMetaDelegate(TiSecurityContext securityContext,
                                FactTypeResolver factTypeResolver,
                                FactResolver factResolver,
                                FactCreateHandler factCreateHandler) {
    this.securityContext = securityContext;
    this.factTypeResolver = factTypeResolver;
    this.factResolver = factResolver;
    this.factCreateHandler = factCreateHandler;
  }

  public Fact handle(CreateMetaFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch referenced Fact and verify that it exists.
    FactRecord referencedFact = factResolver.resolveFact(request.getFact());
    // Verify that user is allowed to access the referenced Fact.
    securityContext.checkReadPermission(referencedFact);

    // Resolve some objects which are required later on. This will also validate those request parameters.
    requestedOrigin = factCreateHandler.resolveOrigin(request.getOrigin());
    requestedOrganization = factCreateHandler.resolveOrganization(request.getOrganization(), requestedOrigin);
    requestedFactType = factTypeResolver.resolveFactType(request.getType());

    // Verify that user is allowed to add Facts for the requested organization.
    securityContext.checkPermission(TiFunctionConstants.addFactObjects, requestedOrganization.getId());

    // Validate that requested Fact matches its FactType.
    factCreateHandler.assertValidFactValue(requestedFactType, request.getValue());
    assertValidFactBinding(request, referencedFact);

    FactRecord newFact = toFactRecord(request, referencedFact);
    return factCreateHandler.saveFact(newFact, request.getComment(), request.getAcl());
  }

  private void assertValidFactBinding(CreateMetaFactRequest request, FactRecord referencedFact) throws InvalidArgumentException {
    // Validate that the referenced Fact has the correct type according to the requested FactType.
    boolean valid = !CollectionUtils.isEmpty(requestedFactType.getRelevantFactBindings()) && requestedFactType.getRelevantFactBindings()
            .stream()
            .anyMatch(b -> Objects.equals(b.getFactTypeID(), referencedFact.getTypeID()));
    if (!valid) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("Requested binding between Facts is not allowed for FactType with id = %s.", requestedFactType.getId()),
                      "invalid.meta.fact.binding", "type", request.getType());
    }
  }

  private FactRecord toFactRecord(CreateMetaFactRequest request, FactRecord referencedFact) throws InvalidArgumentException {
    return new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(requestedFactType.getId())
            .setValue(request.getValue())
            .setInReferenceToID(referencedFact.getId())
            .setOrganizationID(requestedOrganization.getId())
            .setAddedByID(securityContext.getCurrentUserID())
            .setOriginID(requestedOrigin.getId())
            .setTrust(requestedOrigin.getTrust())
            .setConfidence(ObjectUtils.ifNull(request.getConfidence(), requestedFactType.getDefaultConfidence()))
            .setAccessMode(factCreateHandler.resolveAccessMode(referencedFact, request.getAccessMode()))
            .setTimestamp(System.currentTimeMillis())
            .setLastSeenTimestamp(System.currentTimeMillis());
  }
}
