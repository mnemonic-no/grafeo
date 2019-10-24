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
import no.mnemonic.act.platform.service.ti.converters.FactTypeByIdConverter;
import no.mnemonic.act.platform.service.ti.converters.ObjectRecordConverter;
import no.mnemonic.act.platform.service.ti.converters.ObjectTypeByIdConverter;

import javax.inject.Inject;

public class ObjectGetDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final ObjectFactDao objectFactDao;
  private final FactTypeByIdConverter factTypeConverter;
  private final ObjectTypeByIdConverter objectTypeConverter;

  @Inject
  public ObjectGetDelegate(TiSecurityContext securityContext,
                           ObjectFactDao objectFactDao,
                           FactTypeByIdConverter factTypeConverter,
                           ObjectTypeByIdConverter objectTypeConverter) {
    this.securityContext = securityContext;
    this.objectFactDao = objectFactDao;
    this.factTypeConverter = factTypeConverter;
    this.objectTypeConverter = objectTypeConverter;
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
    assertObjectTypeExists(request.getType(), "type");
    ObjectRecord object = objectFactDao.getObject(request.getType(), request.getValue());
    securityContext.checkReadPermission(object);
    return createObjectConverter().apply(object);
  }

  private ObjectRecordConverter createObjectConverter() {
    return new ObjectRecordConverter(objectTypeConverter, factTypeConverter, id -> {
      ObjectStatisticsCriteria criteria = ObjectStatisticsCriteria.builder()
              .addObjectID(id)
              .setCurrentUserID(securityContext.getCurrentUserID())
              .setAvailableOrganizationID(securityContext.getAvailableOrganizationID())
              .build();
      return objectFactDao.calculateObjectStatistics(criteria).getStatistics(id);
    });
  }
}
