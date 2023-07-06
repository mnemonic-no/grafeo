package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.ObjectType;
import no.mnemonic.services.grafeo.api.request.v1.GetObjectTypeByIdRequest;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.ObjectTypeResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.ObjectTypeRequestResolver;

import javax.inject.Inject;

public class ObjectTypeGetByIdDelegate implements Delegate {

  private final GrafeoSecurityContext securityContext;
  private final ObjectTypeResponseConverter objectTypeResponseConverter;
  private final ObjectTypeRequestResolver objectTypeRequestResolver;

  @Inject
  public ObjectTypeGetByIdDelegate(GrafeoSecurityContext securityContext,
                                   ObjectTypeResponseConverter objectTypeResponseConverter,
                                   ObjectTypeRequestResolver objectTypeRequestResolver) {
    this.securityContext = securityContext;
    this.objectTypeResponseConverter = objectTypeResponseConverter;
    this.objectTypeRequestResolver = objectTypeRequestResolver;
  }

  public ObjectType handle(GetObjectTypeByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    securityContext.checkPermission(FunctionConstants.viewGrafeoType);
    return objectTypeResponseConverter.apply(objectTypeRequestResolver.fetchExistingObjectType(request.getId()));
  }
}
