package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.api.request.v1.DeleteOriginRequest;
import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.OriginConverter;
import no.mnemonic.act.platform.service.ti.resolvers.OriginResolver;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;

public class OriginDeleteDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final OriginManager originManager;
  private final OriginResolver originResolver;
  private final OriginConverter originConverter;

  @Inject
  public OriginDeleteDelegate(TiSecurityContext securityContext,
                              OriginManager originManager,
                              OriginResolver originResolver,
                              OriginConverter originConverter) {
    this.securityContext = securityContext;
    this.originManager = originManager;
    this.originResolver = originResolver;
    this.originConverter = originConverter;
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
    return originConverter.apply(entity);
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
      securityContext.checkPermission(TiFunctionConstants.deleteOrigins, entity.getOrganizationID());
    } else {
      // Only check general delete permission.
      securityContext.checkPermission(TiFunctionConstants.deleteOrigins);
    }
  }

  private void assertNotDeleted(OriginEntity entity) throws InvalidArgumentException {
    if (SetUtils.set(entity.getFlags()).contains(OriginEntity.Flag.Deleted)) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("Not allowed to delete an already deleted Origin (id = %s).", entity.getId()),
                      "origin.already.deleted", "id", entity.getId().toString());
    }
  }
}
