package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.Origin;
import no.mnemonic.services.grafeo.api.request.v1.UpdateOriginRequest;
import no.mnemonic.services.grafeo.dao.cassandra.OriginManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.OriginResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.OriginResolver;

import jakarta.inject.Inject;

public class OriginUpdateDelegate implements Delegate {

  private final GrafeoSecurityContext securityContext;
  private final OriginManager originManager;
  private final OriginResolver originResolver;
  private final OriginResponseConverter originResponseConverter;

  @Inject
  public OriginUpdateDelegate(GrafeoSecurityContext securityContext,
                              OriginManager originManager,
                              OriginResolver originResolver,
                              OriginResponseConverter originResponseConverter) {
    this.securityContext = securityContext;
    this.originManager = originManager;
    this.originResolver = originResolver;
    this.originResponseConverter = originResponseConverter;
  }

  public Origin handle(UpdateOriginRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    OriginEntity entity = fetchExistingOrigin(request);

    // Check that user is allowed to access/update the Origin.
    securityContext.checkReadPermission(entity);
    checkUpdatePermission(entity);

    // Check that Origin isn't deleted.
    assertNotDeleted(entity);

    if (request.getOrganization() != null) {
      // Check that the requested organization exists.
      assertOrganizationExists(request);
      // Check update permission for requested organization.
      securityContext.checkPermission(FunctionConstants.updateGrafeoOrigin, request.getOrganization());
      entity.setOrganizationID(request.getOrganization());
    }

    if (!StringUtils.isBlank(request.getName())) {
      // Check that another Origin with the same name doesn't exist.
      assertOriginNotExists(request);
      entity.setName(request.getName());
    }

    if (!StringUtils.isBlank(request.getDescription())) {
      entity.setDescription(request.getDescription());
    }

    if (request.getTrust() != null) {
      entity.setTrust(request.getTrust());
    }

    entity = originManager.saveOrigin(entity);
    return originResponseConverter.apply(entity);
  }

  private OriginEntity fetchExistingOrigin(UpdateOriginRequest request) throws ObjectNotFoundException {
    OriginEntity entity = originResolver.apply(request.getId());
    if (entity == null) {
      throw new ObjectNotFoundException(String.format("Origin with id = %s does not exist.", request.getId()),
              "origin.not.exist", "id", request.getId().toString());
    }
    return entity;
  }

  private void checkUpdatePermission(OriginEntity entity) throws AccessDeniedException, AuthenticationFailedException {
    if (entity.getOrganizationID() != null) {
      // Check specific update permission for the Origin.
      securityContext.checkPermission(FunctionConstants.updateGrafeoOrigin, entity.getOrganizationID());
    } else {
      // Only check general update permission.
      securityContext.checkPermission(FunctionConstants.updateGrafeoOrigin);
    }
  }

  private void assertNotDeleted(OriginEntity entity) throws InvalidArgumentException {
    if (entity.isSet(OriginEntity.Flag.Deleted)) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("Not allowed to update deleted Origin (id = %s).", entity.getId()),
                      "origin.update.deleted", "id", entity.getId().toString());
    }
  }

  private void assertOriginNotExists(UpdateOriginRequest request) throws InvalidArgumentException {
    // It's not allowed that two Origins have the same name.
    if (originManager.getOrigin(request.getName()) != null) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("Origin with name = %s already exists.", request.getName()),
                      "origin.exist", "name", request.getName());
    }
  }

  private void assertOrganizationExists(UpdateOriginRequest request) throws InvalidArgumentException {
    // This just verifies that the user has somehow access to the requested organization.
    // More specific access control must be done afterwards.
    if (!securityContext.getAvailableOrganizationID().contains(request.getOrganization())) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("Organization with id = %s does not exist.", request.getOrganization()),
                      "organization.not.exist", "organization", request.getOrganization().toString());
    }
  }
}
