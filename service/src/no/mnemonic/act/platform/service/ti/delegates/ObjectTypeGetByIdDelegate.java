package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.GetObjectTypeByIdRequest;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.ObjectTypeConverter;
import no.mnemonic.act.platform.service.ti.resolvers.ObjectTypeResolver;

import javax.inject.Inject;

public class ObjectTypeGetByIdDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final ObjectTypeConverter objectTypeConverter;
  private final ObjectTypeResolver objectTypeResolver;

  @Inject
  public ObjectTypeGetByIdDelegate(TiSecurityContext securityContext,
                                   ObjectTypeConverter objectTypeConverter,
                                   ObjectTypeResolver objectTypeResolver) {
    this.securityContext = securityContext;
    this.objectTypeConverter = objectTypeConverter;
    this.objectTypeResolver = objectTypeResolver;
  }

  public ObjectType handle(GetObjectTypeByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    securityContext.checkPermission(TiFunctionConstants.viewTypes);
    return objectTypeConverter.apply(objectTypeResolver.fetchExistingObjectType(request.getId()));
  }
}
