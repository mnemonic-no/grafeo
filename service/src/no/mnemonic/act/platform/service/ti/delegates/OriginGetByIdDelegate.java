package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.api.request.v1.GetOriginByIdRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class OriginGetByIdDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final Function<UUID, OriginEntity> originResolver;
  private final Function<OriginEntity, Origin> originConverter;

  @Inject
  public OriginGetByIdDelegate(TiSecurityContext securityContext,
                               Function<UUID, OriginEntity> originResolver,
                               Function<OriginEntity, Origin> originConverter) {
    this.securityContext = securityContext;
    this.originResolver = originResolver;
    this.originConverter = originConverter;
  }

  public Origin handle(GetOriginByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Verify that the user is allowed to view Origins in general.
    securityContext.checkPermission(TiFunctionConstants.viewOrigins);

    // Fetch Origin and verify that it exists.
    OriginEntity entity = originResolver.apply(request.getId());
    if (entity == null) {
      throw new ObjectNotFoundException(String.format("Origin with id = %s does not exist.", request.getId()),
              "origin.not.exist", "id", request.getId().toString());
    }

    if (entity.getOrganizationID() != null) {
      // Verify that the user is allowed to view the fetched Origin if it belongs to an organization.
      securityContext.checkPermission(TiFunctionConstants.viewOrigins, entity.getOrganizationID());
    }

    return originConverter.apply(entity);
  }
}
