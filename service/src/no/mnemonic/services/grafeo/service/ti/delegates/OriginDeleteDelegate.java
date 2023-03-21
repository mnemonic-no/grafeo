package no.mnemonic.services.grafeo.service.ti.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.Origin;
import no.mnemonic.services.grafeo.api.request.v1.DeleteOriginRequest;
import no.mnemonic.services.grafeo.dao.cassandra.OriginManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.service.ti.TiFunctionConstants;
import no.mnemonic.services.grafeo.service.ti.TiSecurityContext;
import no.mnemonic.services.grafeo.service.ti.converters.response.OriginResponseConverter;
import no.mnemonic.services.grafeo.service.ti.resolvers.OriginResolver;

import javax.inject.Inject;

public class OriginDeleteDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final OriginManager originManager;
  private final OriginResolver originResolver;
  private final OriginResponseConverter originResponseConverter;

  @Inject
  public OriginDeleteDelegate(TiSecurityContext securityContext,
                              OriginManager originManager,
                              OriginResolver originResolver,
                              OriginResponseConverter originResponseConverter) {
    this.securityContext = securityContext;
    this.originManager = originManager;
    this.originResolver = originResolver;
    this.originResponseConverter = originResponseConverter;
  }

  public Origin handle(DeleteOriginRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    OriginEntity entity = fetchExistingOrigin(request);

    // Check that user is allowed to access/delete the Origin.
    securityContext.checkReadPermission(entity);
    checkDeletePermission(entity);

    // Check that Origin isn't already deleted.
    assertNotDeleted(entity);

    // Mark Origin as deleted.
    entity.addFlag(OriginEntity.Flag.Deleted);

    entity = originManager.saveOrigin(entity);
    return originResponseConverter.apply(entity);
  }

  private OriginEntity fetchExistingOrigin(DeleteOriginRequest request) throws ObjectNotFoundException {
    OriginEntity entity = originResolver.apply(request.getId());
    if (entity == null) {
      throw new ObjectNotFoundException(String.format("Origin with id = %s does not exist.", request.getId()),
              "origin.not.exist", "id", request.getId().toString());
    }
    return entity;
  }

  private void checkDeletePermission(OriginEntity entity) throws AccessDeniedException, AuthenticationFailedException {
    if (entity.getOrganizationID() != null) {
      // Check specific delete permission for the Origin.
      securityContext.checkPermission(TiFunctionConstants.deleteThreatIntelOrigin, entity.getOrganizationID());
    } else {
      // Only check general delete permission.
      securityContext.checkPermission(TiFunctionConstants.deleteThreatIntelOrigin);
    }
  }

  private void assertNotDeleted(OriginEntity entity) throws InvalidArgumentException {
    if (entity.isSet(OriginEntity.Flag.Deleted)) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("Not allowed to delete an already deleted Origin (id = %s).", entity.getId()),
                      "origin.already.deleted", "id", entity.getId().toString());
    }
  }
}
