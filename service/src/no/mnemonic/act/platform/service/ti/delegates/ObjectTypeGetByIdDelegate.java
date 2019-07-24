package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.GetObjectTypeByIdRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;

import javax.inject.Inject;
import java.util.function.Function;

public class ObjectTypeGetByIdDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final Function<ObjectTypeEntity, ObjectType> objectTypeConverter;

  @Inject
  public ObjectTypeGetByIdDelegate(TiSecurityContext securityContext,
                                   Function<ObjectTypeEntity, ObjectType> objectTypeConverter) {
    this.securityContext = securityContext;
    this.objectTypeConverter = objectTypeConverter;
  }

  public ObjectType handle(GetObjectTypeByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    securityContext.checkPermission(TiFunctionConstants.viewTypes);
    return objectTypeConverter.apply(fetchExistingObjectType(request.getId()));
  }
}
