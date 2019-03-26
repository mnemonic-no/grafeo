package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchMetaFactsRequest;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.SearchMetaFactsRequestConverter;
import no.mnemonic.act.platform.service.ti.handlers.FactSearchHandler;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.common.api.ResultSet;

public class FactSearchMetaDelegate extends AbstractDelegate {

  private final FactSearchHandler factSearchHandler;

  private FactSearchMetaDelegate(FactSearchHandler factSearchHandler) {
    this.factSearchHandler = factSearchHandler;
  }

  public ResultSet<Fact> handle(SearchMetaFactsRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Verify that user is allowed to view Facts.
    TiSecurityContext.get().checkPermission(TiFunctionConstants.viewFactObjects);
    // Fetch referenced Fact, verify that it exists and that user is allowed to access the Fact.
    TiSecurityContext.get().checkReadPermission(fetchExistingFact(request.getFact()));
    // Search for meta Facts bound to the referenced Fact.
    return factSearchHandler.search(toCriteria(request), request.getIncludeRetracted());
  }

  private FactSearchCriteria toCriteria(SearchMetaFactsRequest request) {
    return SearchMetaFactsRequestConverter.builder()
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

    public FactSearchMetaDelegate build() {
      ObjectUtils.notNull(factSearchHandler, "Cannot instantiate FactSearchMetaDelegate without 'factSearchHandler'.");
      return new FactSearchMetaDelegate(factSearchHandler);
    }

    public Builder setFactSearchHandler(FactSearchHandler factSearchHandler) {
      this.factSearchHandler = factSearchHandler;
      return this;
    }
  }
}
