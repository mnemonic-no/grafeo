package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver.RETRACTION_FACT_TYPE_ID;

public class ObjectSearchFactsDelegate extends AbstractDelegate {

  private static final int DEFAULT_LIMIT = 25;

  public static ObjectSearchFactsDelegate create() {
    return new ObjectSearchFactsDelegate();
  }

  public ResultSet<Fact> handle(SearchObjectFactsRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    TiSecurityContext.get().checkPermission(TiFunctionConstants.viewFactObjects);
    assertRequest(request);

    List<FactEntity> resolvedFacts = checkObjectAccess(resolveObject(request)); // All accessible Facts.
    List<FactEntity> filteredFacts = filterFacts(resolvedFacts, request); // Filtered Facts based on request.
    List<FactEntity> limitedFacts = limitFacts(filteredFacts, request); // Final result after applying limit.

    return ResultSet.builder()
            .setCount(filteredFacts.size())
            .setLimit(ObjectUtils.ifNull(request.getLimit(), DEFAULT_LIMIT))
            .setValues(limitedFacts.stream().map(TiRequestContext.get().getFactConverter()).collect(Collectors.toList()))
            .build();
  }

  private void assertRequest(SearchObjectFactsRequest request) throws InvalidArgumentException {
    // This isn't a user error as those fields should be set by the REST endpoint, just make sure they are set.
    if (request.getObjectID() == null && StringUtils.isBlank(request.getObjectType()) && StringUtils.isBlank(request.getObjectValue())) {
      throw new IllegalArgumentException("Cannot fetch Object, neither 'objectID' nor 'objectType' and 'objectValue' are set in request.");
    }

    // Make sure that ObjectType exists. Assume to fetch Object by type/value if 'objectID' isn't set in request.
    if (request.getObjectID() == null) {
      assertObjectTypeExists(request.getObjectType(), "objectType");
    }
  }

  private ObjectEntity resolveObject(SearchObjectFactsRequest request) {
    ObjectEntity object;

    if (request.getObjectID() != null) {
      object = TiRequestContext.get().getObjectManager().getObject(request.getObjectID());
    } else {
      object = TiRequestContext.get().getObjectManager().getObject(request.getObjectType(), request.getObjectValue());
    }

    return object;
  }

  private List<FactEntity> filterFacts(List<FactEntity> facts, SearchObjectFactsRequest request) {
    return facts.stream()
            .filter(factTypeFilter(request.getFactType()))
            .filter(factValueFilter(request.getFactValue()))
            .filter(sourceFilter(request.getSource()))
            .filter(includeRetractedFilter(request.getIncludeRetracted(), facts))
            .filter(beforeFilter(request.getBefore()))
            .filter(afterFilter(request.getAfter()))
            .collect(Collectors.toList());
  }

  private List<FactEntity> limitFacts(List<FactEntity> facts, SearchObjectFactsRequest request) {
    int limit = ObjectUtils.ifNull(request.getLimit(), DEFAULT_LIMIT);
    return limit == 0 ? facts : facts.stream().limit(limit).collect(Collectors.toList());
  }

  private Predicate<FactEntity> includeRetractedFilter(Boolean includeRetracted, List<FactEntity> allFacts) {
    return fact -> {
      // The retraction Fact is bound to the same Object than the Fact filtered here, thus, it will be in the list
      // of all (unfiltered) Facts. If not the user doesn't have access to it which means that from the user's
      // point of view the Fact isn't retracted.
      boolean isRetracted = allFacts.stream().anyMatch(f -> Objects.equals(f.getTypeID(), RETRACTION_FACT_TYPE_ID) && Objects.equals(f.getInReferenceToID(), fact.getId()));
      return ObjectUtils.ifNull(includeRetracted, false) || !isRetracted;
    };
  }

}
