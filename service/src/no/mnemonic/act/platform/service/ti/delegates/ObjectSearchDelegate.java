package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class ObjectSearchDelegate extends AbstractDelegate {

  private static final int DEFAULT_LIMIT = 25;

  public static ObjectSearchDelegate create() {
    return new ObjectSearchDelegate();
  }

  public ResultSet<Object> handle(SearchObjectRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    TiSecurityContext.get().checkPermission(TiFunctionConstants.viewFactObjects);

    List<ObjectEntity> filteredObjects = filterObjects(request);

    return ResultSet.<Object>builder()
            .setCount(filteredObjects.size())
            .setLimit(ObjectUtils.ifNull(request.getLimit(), DEFAULT_LIMIT))
            .setValues(filteredObjects.stream().map(TiRequestContext.get().getObjectConverter()).collect(Collectors.toList()))
            .build();
  }

  private List<ObjectEntity> filterObjects(SearchObjectRequest request) throws AccessDeniedException {
    int limit = determineLimit(request);
    List<ObjectEntity> filteredObjects = new ArrayList<>();

    // Page through all Objects until we have enough results. This means that the returned 'count' will be too small,
    // but this is necessary in order to avoid fetching the whole database.
    Iterator<ObjectEntity> objectIterator = TiRequestContext.get().getObjectManager().fetchObjects();
    while (objectIterator.hasNext() && filteredObjects.size() < limit) {
      ObjectEntity object = objectIterator.next();
      if (!objectMatchesSearchRequest(request, object)) continue;
      if (!factMatchesSearchRequest(request, object)) continue;

      filteredObjects.add(object);
    }

    return filteredObjects;
  }

  private int determineLimit(SearchObjectRequest request) throws AccessDeniedException {
    int limit = ObjectUtils.ifNull(request.getLimit(), DEFAULT_LIMIT);

    // Don't allow unlimited search for now as this will cause fetching the whole database.
    if (limit == 0) {
      throw new AccessDeniedException("Unlimited search is not permitted.");
    }

    return limit;
  }

  private boolean objectMatchesSearchRequest(SearchObjectRequest request, ObjectEntity object) {
    ObjectTypeEntity type = TiRequestContext.get().getObjectManager().getObjectType(object.getTypeID());
    if (!CollectionUtils.isEmpty(request.getObjectType()) && !request.getObjectType().contains(type.getName())) {
      return false;
    }

    if (!CollectionUtils.isEmpty(request.getObjectValue()) && !request.getObjectValue().contains(object.getValue())) {
      return false;
    }

    return true;
  }

  private boolean factMatchesSearchRequest(SearchObjectRequest request, ObjectEntity object) {
    return resolveFactsForObject(object.getId()).stream()
            .filter(factTypeFilter(request.getFactType()))
            .filter(factValueFilter(request.getFactValue()))
            .filter(sourceFilter(request.getSource()))
            .filter(beforeFilter(request.getBefore()))
            .filter(afterFilter(request.getAfter()))
            .count() > 0;
  }

}
