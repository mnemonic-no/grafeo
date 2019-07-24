package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.api.request.v1.GetObjectByIdRequest;
import no.mnemonic.act.platform.api.request.v1.GetObjectByTypeValueRequest;
import no.mnemonic.act.platform.dao.api.ObjectStatisticsCriteria;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.ObjectConverter;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class ObjectGetDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final ObjectManager objectManager;
  private final FactSearchManager factSearchManager;
  private final Function<UUID, FactType> factTypeConverter;
  private final Function<UUID, ObjectType> objectTypeConverter;

  @Inject
  public ObjectGetDelegate(TiSecurityContext securityContext,
                           ObjectManager objectManager,
                           FactSearchManager factSearchManager,
                           Function<UUID, FactType> factTypeConverter,
                           Function<UUID, ObjectType> objectTypeConverter) {
    this.securityContext = securityContext;
    this.objectManager = objectManager;
    this.factSearchManager = factSearchManager;
    this.factTypeConverter = factTypeConverter;
    this.objectTypeConverter = objectTypeConverter;
  }

  public Object handle(GetObjectByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.viewFactObjects);
    ObjectEntity object = objectManager.getObject(request.getId());
    securityContext.checkReadPermission(object);
    return createObjectConverter().apply(object);
  }

  public Object handle(GetObjectByTypeValueRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.viewFactObjects);
    assertObjectTypeExists(request.getType(), "type");
    ObjectEntity object = objectManager.getObject(request.getType(), request.getValue());
    securityContext.checkReadPermission(object);
    return createObjectConverter().apply(object);
  }

  private ObjectConverter createObjectConverter() {
    return new ObjectConverter(objectTypeConverter, factTypeConverter, id -> {
      ObjectStatisticsCriteria criteria = ObjectStatisticsCriteria.builder()
              .addObjectID(id)
              .setCurrentUserID(securityContext.getCurrentUserID())
              .setAvailableOrganizationID(securityContext.getAvailableOrganizationID())
              .build();
      return factSearchManager.calculateObjectStatistics(criteria).getStatistics(id);
    });
  }
}
