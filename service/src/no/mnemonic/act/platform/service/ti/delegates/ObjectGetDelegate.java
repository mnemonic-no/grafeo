package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.request.v1.GetObjectByIdRequest;
import no.mnemonic.act.platform.api.request.v1.GetObjectByTypeValueRequest;
import no.mnemonic.act.platform.dao.api.ObjectStatisticsCriteria;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.ObjectConverter;

public class ObjectGetDelegate extends AbstractDelegate {

  public static ObjectGetDelegate create() {
    return new ObjectGetDelegate();
  }

  public Object handle(GetObjectByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    TiSecurityContext.get().checkPermission(TiFunctionConstants.viewFactObjects);
    ObjectEntity object = TiRequestContext.get().getObjectManager().getObject(request.getId());
    TiSecurityContext.get().checkReadPermission(object);
    return createObjectConverter().apply(object);
  }

  public Object handle(GetObjectByTypeValueRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    TiSecurityContext.get().checkPermission(TiFunctionConstants.viewFactObjects);
    assertObjectTypeExists(request.getType(), "type");
    ObjectEntity object = TiRequestContext.get().getObjectManager().getObject(request.getType(), request.getValue());
    TiSecurityContext.get().checkReadPermission(object);
    return createObjectConverter().apply(object);
  }

  private ObjectConverter createObjectConverter() {
    return ObjectConverter.builder()
            .setObjectTypeConverter(id -> {
              ObjectTypeEntity type = TiRequestContext.get().getObjectManager().getObjectType(id);
              return TiRequestContext.get().getObjectTypeConverter().apply(type);
            })
            .setFactTypeConverter(id -> {
              FactTypeEntity type = TiRequestContext.get().getFactManager().getFactType(id);
              return TiRequestContext.get().getFactTypeConverter().apply(type);
            })
            .setFactStatisticsResolver(id -> {
              ObjectStatisticsCriteria criteria = ObjectStatisticsCriteria.builder()
                      .addObjectID(id)
                      .setCurrentUserID(TiSecurityContext.get().getCurrentUserID())
                      .setAvailableOrganizationID(TiSecurityContext.get().getAvailableOrganizationID())
                      .build();
              return TiRequestContext.get().getFactSearchManager().calculateObjectStatistics(criteria).getStatistics(id);
            })
            .build();
  }

}
