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
import no.mnemonic.act.platform.service.ti.converters.response.OriginResponseConverter;
import no.mnemonic.act.platform.service.ti.resolvers.OriginResolver;

import javax.inject.Inject;

public class OriginGetByIdDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final OriginResolver originResolver;
  private final OriginResponseConverter originResponseConverter;

  @Inject
  public OriginGetByIdDelegate(TiSecurityContext securityContext,
                               OriginResolver originResolver,
                               OriginResponseConverter originResponseConverter) {
    this.securityContext = securityContext;
    this.originResolver = originResolver;
    this.originResponseConverter = originResponseConverter;
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

    // Verify that the user is allowed to view the fetched Origin.
    securityContext.checkReadPermission(entity);

    return originResponseConverter.apply(entity);
  }
}
