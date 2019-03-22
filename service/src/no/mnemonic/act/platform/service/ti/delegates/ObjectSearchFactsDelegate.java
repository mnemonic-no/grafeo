package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.SearchObjectFactsRequestConverter;
import no.mnemonic.act.platform.service.ti.handlers.FactSearchHandler;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.services.common.api.ResultSet;

public class ObjectSearchFactsDelegate extends AbstractDelegate {

  private final FactSearchHandler factSearchHandler;

  private ObjectSearchFactsDelegate(FactSearchHandler factSearchHandler) {
    this.factSearchHandler = factSearchHandler;
  }

  public ResultSet<Fact> handle(SearchObjectFactsRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    TiSecurityContext.get().checkPermission(TiFunctionConstants.viewFactObjects);
    assertRequest(request);
    // Resolve Object based on parameters set in request.
    ObjectEntity object = resolveObject(request);
    // Check access to Object. This will throw an AccessDeniedException if Object doesn't exist.
    TiSecurityContext.get().checkReadPermission(object);
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

  private FactSearchCriteria toCriteria(SearchObjectFactsRequest request, ObjectEntity object) {
    // Make sure to only search by the ID of the resolved Object.
    request = request.setObjectID(object.getId())
            .setObjectType(null)
            .setObjectValue(null);

    return SearchObjectFactsRequestConverter.builder()
            .setCurrentUserIdSupplier(() -> TiSecurityContext.get().getCurrentUserID())
            .setAvailableOrganizationIdSupplier(() -> TiSecurityContext.get().getAvailableOrganizationID())
            .build()
            .apply(request);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private FactSearchHandler factSearchHandler;

    private Builder() {
    }

    public ObjectSearchFactsDelegate build() {
      ObjectUtils.notNull(factSearchHandler, "Cannot instantiate ObjectSearchFactsDelegate without 'factSearchHandler'.");
      return new ObjectSearchFactsDelegate(factSearchHandler);
    }

    public Builder setFactSearchHandler(FactSearchHandler factSearchHandler) {
      this.factSearchHandler = factSearchHandler;
      return this;
    }
  }
}
