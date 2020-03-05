package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.api.request.v1.CreateOriginRequest;
import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.OriginConverter;

import javax.inject.Inject;
import java.util.UUID;

import static no.mnemonic.act.platform.service.ti.ThreatIntelligenceServiceImpl.GLOBAL_NAMESPACE;

public class OriginCreateDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final OriginManager originManager;
  private final OriginConverter originConverter;

  @Inject
  public OriginCreateDelegate(TiSecurityContext securityContext,
                              OriginManager originManager,
                              OriginConverter originConverter) {
    this.securityContext = securityContext;
    this.originManager = originManager;
    this.originConverter = originConverter;
  }

  public Origin handle(CreateOriginRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    if (request.getOrganization() != null) {
      // Check that that the requested organization exists.
      assertOrganizationExists(request);
      // Check add permission for requested organization.
      securityContext.checkPermission(TiFunctionConstants.addOrigins, request.getOrganization());
    } else {
      // Only check general add permission.
      securityContext.checkPermission(TiFunctionConstants.addOrigins);
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
    return originConverter.apply(entity);
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
