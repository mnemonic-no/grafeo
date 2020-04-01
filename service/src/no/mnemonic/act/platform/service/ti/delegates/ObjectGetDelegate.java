package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.request.v1.GetObjectByIdRequest;
import no.mnemonic.act.platform.api.request.v1.GetObjectByTypeValueRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.ObjectStatisticsCriteria;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.resolvers.response.FactTypeByIdResponseResolver;
import no.mnemonic.act.platform.service.ti.converters.response.ObjectResponseConverter;
import no.mnemonic.act.platform.service.ti.resolvers.response.ObjectTypeByIdResponseResolver;
import no.mnemonic.act.platform.service.ti.handlers.ObjectTypeHandler;

import javax.inject.Inject;

public class ObjectGetDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final ObjectFactDao objectFactDao;
  private final FactTypeByIdResponseResolver factTypeConverter;
  private final ObjectTypeByIdResponseResolver objectTypeConverter;
  private final ObjectTypeHandler objectTypeHandler;

  @Inject
  public ObjectGetDelegate(TiSecurityContext securityContext,
                           ObjectFactDao objectFactDao,
                           FactTypeByIdResponseResolver factTypeConverter,
                           ObjectTypeByIdResponseResolver objectTypeConverter,
                           ObjectTypeHandler objectTypeHandler) {
    this.securityContext = securityContext;
    this.objectFactDao = objectFactDao;
    this.factTypeConverter = factTypeConverter;
    this.objectTypeConverter = objectTypeConverter;
    this.objectTypeHandler = objectTypeHandler;
  }

  public Object handle(GetObjectByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.viewFactObjects);
    ObjectRecord object = objectFactDao.getObject(request.getId());
    securityContext.checkReadPermission(object);
    return createObjectConverter().apply(object);
  }

  public Object handle(GetObjectByTypeValueRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.viewFactObjects);
    objectTypeHandler.assertObjectTypeExists(request.getType(), "type");
    ObjectRecord object = objectFactDao.getObject(request.getType(), request.getValue());
    securityContext.checkReadPermission(object);
    return createObjectConverter().apply(object);
  }

  private ObjectResponseConverter createObjectConverter() {
    return new ObjectResponseConverter(objectTypeConverter, factTypeConverter, id -> {
      ObjectStatisticsCriteria criteria = ObjectStatisticsCriteria.builder()
              .addObjectID(id)
              .setCurrentUserID(securityContext.getCurrentUserID())
              .setAvailableOrganizationID(securityContext.getAvailableOrganizationID())
              .build();
      return objectFactDao.calculateObjectStatistics(criteria).getStatistics(id);
    });
  }
}
