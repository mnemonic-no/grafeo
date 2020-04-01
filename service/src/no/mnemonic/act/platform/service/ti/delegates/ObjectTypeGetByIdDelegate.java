package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.GetObjectTypeByIdRequest;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.ObjectTypeResponseConverter;
import no.mnemonic.act.platform.service.ti.resolvers.request.ObjectTypeRequestResolver;

import javax.inject.Inject;

public class ObjectTypeGetByIdDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final ObjectTypeResponseConverter objectTypeResponseConverter;
  private final ObjectTypeRequestResolver objectTypeRequestResolver;

  @Inject
  public ObjectTypeGetByIdDelegate(TiSecurityContext securityContext,
                                   ObjectTypeResponseConverter objectTypeResponseConverter,
                                   ObjectTypeRequestResolver objectTypeRequestResolver) {
    this.securityContext = securityContext;
    this.objectTypeResponseConverter = objectTypeResponseConverter;
    this.objectTypeRequestResolver = objectTypeRequestResolver;
  }

  public ObjectType handle(GetObjectTypeByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    securityContext.checkPermission(TiFunctionConstants.viewTypes);
    return objectTypeResponseConverter.apply(objectTypeRequestResolver.fetchExistingObjectType(request.getId()));
  }
}
