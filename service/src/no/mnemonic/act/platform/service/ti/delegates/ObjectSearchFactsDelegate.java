package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.request.SearchObjectFactsRequestConverter;
import no.mnemonic.act.platform.service.ti.handlers.FactSearchHandler;
import no.mnemonic.act.platform.service.ti.handlers.ObjectTypeHandler;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;

public class ObjectSearchFactsDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final ObjectFactDao objectFactDao;
  private final SearchObjectFactsRequestConverter requestConverter;
  private final FactSearchHandler factSearchHandler;
  private final ObjectTypeHandler objectTypeHandler;

  @Inject
  public ObjectSearchFactsDelegate(TiSecurityContext securityContext,
                                   ObjectFactDao objectFactDao,
                                   SearchObjectFactsRequestConverter requestConverter,
                                   FactSearchHandler factSearchHandler,
                                   ObjectTypeHandler objectTypeHandler) {
    this.securityContext = securityContext;
    this.objectFactDao = objectFactDao;
    this.requestConverter = requestConverter;
    this.factSearchHandler = factSearchHandler;
    this.objectTypeHandler = objectTypeHandler;
  }

  public ResultSet<Fact> handle(SearchObjectFactsRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.viewThreatIntelFact);
    assertRequest(request);
    // Resolve Object based on parameters set in request.
    ObjectRecord object = resolveObject(request);
    // Check access to Object. This will throw an AccessDeniedException if Object doesn't exist.
    securityContext.checkReadPermission(object);
    // Search for Facts bound to the resolved Object.
    return factSearchHandler.search(toCriteria(request, object), request.getIncludeRetracted());
  }

  private void assertRequest(SearchObjectFactsRequest request) throws InvalidArgumentException {
    // This isn't a user error as those fields should be set by the REST endpoint, just make sure they are set.
    if (request.getObjectID() == null && StringUtils.isBlank(request.getObjectType()) && StringUtils.isBlank(request.getObjectValue())) {
      throw new IllegalArgumentException("Cannot fetch Object, neither 'objectID' nor 'objectType' and 'objectValue' are set in request.");
    }

    // Make sure that ObjectType exists. Assume to fetch Object by type/value if 'objectID' isn't set in request.
    if (request.getObjectID() == null) {
      objectTypeHandler.assertObjectTypeExists(request.getObjectType(), "objectType");
    }
  }

  private ObjectRecord resolveObject(SearchObjectFactsRequest request) {
    ObjectRecord object;

    if (request.getObjectID() != null) {
      object = objectFactDao.getObject(request.getObjectID());
    } else {
      object = objectFactDao.getObject(request.getObjectType(), request.getObjectValue());
    }

    return object;
  }

  private FactSearchCriteria toCriteria(SearchObjectFactsRequest request, ObjectRecord object) throws InvalidArgumentException {
    // Make sure to only search by the ID of the resolved Object.
    request = request.setObjectID(object.getId())
            .setObjectType(null)
            .setObjectValue(null);

    return requestConverter.apply(request);
  }
}
