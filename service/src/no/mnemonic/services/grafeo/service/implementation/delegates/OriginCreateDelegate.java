package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.model.v1.Origin;
import no.mnemonic.services.grafeo.api.request.v1.CreateOriginRequest;
import no.mnemonic.services.grafeo.dao.cassandra.OriginManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.OriginResponseConverter;

import jakarta.inject.Inject;
import java.util.UUID;

import static no.mnemonic.services.grafeo.service.implementation.GrafeoServiceImpl.GLOBAL_NAMESPACE;

public class OriginCreateDelegate implements Delegate {

  private final GrafeoSecurityContext securityContext;
  private final OriginManager originManager;
  private final OriginResponseConverter originResponseConverter;

  @Inject
  public OriginCreateDelegate(GrafeoSecurityContext securityContext,
                              OriginManager originManager,
                              OriginResponseConverter originResponseConverter) {
    this.securityContext = securityContext;
    this.originManager = originManager;
    this.originResponseConverter = originResponseConverter;
  }

  public Origin handle(CreateOriginRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    if (request.getOrganization() != null) {
      // Check that that the requested organization exists.
      assertOrganizationExists(request);
      // Check add permission for requested organization.
      securityContext.checkPermission(FunctionConstants.addGrafeoOrigin, request.getOrganization());
    } else {
      // Only check general add permission.
      securityContext.checkPermission(FunctionConstants.addGrafeoOrigin);
    }

    // Check that Origin doesn't exist yet (by name).
    assertOriginNotExists(request);

    OriginEntity entity = new OriginEntity()
            .setId(UUID.randomUUID()) // ID needs to be provided by client.
            .setNamespaceID(GLOBAL_NAMESPACE) // For now everything will just be part of the global namespace.
            .setOrganizationID(request.getOrganization())
            .setName(request.getName())
            .setDescription(request.getDescription())
            .setTrust(request.getTrust())
            .setType(OriginEntity.Type.Group);

    entity = originManager.saveOrigin(entity);
    return originResponseConverter.apply(entity);
  }

  private void assertOriginNotExists(CreateOriginRequest request) throws InvalidArgumentException {
    // It's not allowed that two Origins have the same name.
    if (originManager.getOrigin(request.getName()) != null) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("Origin with name = %s already exists.", request.getName()),
                      "origin.exist", "name", request.getName());
    }
  }

  private void assertOrganizationExists(CreateOriginRequest request) throws InvalidArgumentException {
    // This just verifies that the user has somehow access to the requested organization.
    // More specific access control must be done afterwards.
    if (!securityContext.getAvailableOrganizationID().contains(request.getOrganization())) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("Organization with id = %s does not exist.", request.getOrganization()),
                      "organization.not.exist", "organization", request.getOrganization().toString());
    }
  }
}
