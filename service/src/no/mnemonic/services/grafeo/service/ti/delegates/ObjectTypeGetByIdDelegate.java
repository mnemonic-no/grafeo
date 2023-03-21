package no.mnemonic.services.grafeo.service.ti.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.ObjectType;
import no.mnemonic.services.grafeo.api.request.v1.GetObjectTypeByIdRequest;
import no.mnemonic.services.grafeo.service.ti.TiFunctionConstants;
import no.mnemonic.services.grafeo.service.ti.TiSecurityContext;
import no.mnemonic.services.grafeo.service.ti.converters.response.ObjectTypeResponseConverter;
import no.mnemonic.services.grafeo.service.ti.resolvers.request.ObjectTypeRequestResolver;

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
    securityContext.checkPermission(TiFunctionConstants.viewThreatIntelType);
    return objectTypeResponseConverter.apply(objectTypeRequestResolver.fetchExistingObjectType(request.getId()));
  }
}
