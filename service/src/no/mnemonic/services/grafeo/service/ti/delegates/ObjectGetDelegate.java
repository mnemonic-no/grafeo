package no.mnemonic.services.grafeo.service.ti.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.model.v1.Object;
import no.mnemonic.services.grafeo.api.request.v1.GetObjectByIdRequest;
import no.mnemonic.services.grafeo.api.request.v1.GetObjectByTypeValueRequest;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.ObjectStatisticsCriteria;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.service.ti.TiFunctionConstants;
import no.mnemonic.services.grafeo.service.ti.TiSecurityContext;
import no.mnemonic.services.grafeo.service.ti.converters.response.ObjectResponseConverter;
import no.mnemonic.services.grafeo.service.ti.handlers.ObjectTypeHandler;
import no.mnemonic.services.grafeo.service.ti.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.services.grafeo.service.ti.resolvers.IndexSelectCriteriaResolver;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.FactTypeByIdResponseResolver;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.ObjectTypeByIdResponseResolver;

import javax.inject.Inject;

public class ObjectGetDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final AccessControlCriteriaResolver accessControlCriteriaResolver;
  private final IndexSelectCriteriaResolver indexSelectCriteriaResolver;
  private final ObjectFactDao objectFactDao;
  private final FactTypeByIdResponseResolver factTypeConverter;
  private final ObjectTypeByIdResponseResolver objectTypeConverter;
  private final ObjectTypeHandler objectTypeHandler;

  @Inject
  public ObjectGetDelegate(TiSecurityContext securityContext,
                           AccessControlCriteriaResolver accessControlCriteriaResolver,
                           IndexSelectCriteriaResolver indexSelectCriteriaResolver,
                           ObjectFactDao objectFactDao,
                           FactTypeByIdResponseResolver factTypeConverter,
                           ObjectTypeByIdResponseResolver objectTypeConverter,
                           ObjectTypeHandler objectTypeHandler) {
    this.securityContext = securityContext;
    this.accessControlCriteriaResolver = accessControlCriteriaResolver;
    this.indexSelectCriteriaResolver = indexSelectCriteriaResolver;
    this.objectFactDao = objectFactDao;
    this.factTypeConverter = factTypeConverter;
    this.objectTypeConverter = objectTypeConverter;
    this.objectTypeHandler = objectTypeHandler;
  }

  public Object handle(GetObjectByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.viewThreatIntelFact);
    ObjectRecord object = objectFactDao.getObject(request.getId());
    securityContext.checkReadPermission(object);
    return createObjectConverter(request.getAfter(), request.getBefore()).apply(object);
  }

  public Object handle(GetObjectByTypeValueRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.viewThreatIntelFact);
    objectTypeHandler.assertObjectTypeExists(request.getType(), "type");
    ObjectRecord object = objectFactDao.getObject(request.getType(), request.getValue());
    securityContext.checkReadPermission(object);
    return createObjectConverter(request.getAfter(), request.getBefore()).apply(object);
  }

  private ObjectResponseConverter createObjectConverter(Long after, Long before) throws InvalidArgumentException {
    IndexSelectCriteria indexSelectCriteria = indexSelectCriteriaResolver.validateAndCreateCriteria(after, before);

    return new ObjectResponseConverter(objectTypeConverter, factTypeConverter, id -> {
      ObjectStatisticsCriteria criteria = ObjectStatisticsCriteria.builder()
              .addObjectID(id)
              .setStartTimestamp(after)
              .setEndTimestamp(before)
              .setAccessControlCriteria(accessControlCriteriaResolver.get())
              .setIndexSelectCriteria(indexSelectCriteria)
              .build();
      return objectFactDao.calculateObjectStatistics(criteria).getStatistics(id);
    });
  }
}
