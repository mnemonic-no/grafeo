package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchFactRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.SearchFactRequestConverter;
import no.mnemonic.act.platform.service.ti.handlers.FactSearchHandler;
import no.mnemonic.commons.utilities.ObjectUtils;

public class FactSearchDelegate extends AbstractDelegate {

  private final FactSearchHandler factSearchHandler;

  private FactSearchDelegate(FactSearchHandler factSearchHandler) {
    this.factSearchHandler = factSearchHandler;
  }

  public ResultSet<Fact> handle(SearchFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    TiSecurityContext.get().checkPermission(TiFunctionConstants.viewFactObjects);
    return factSearchHandler.search(toCriteria(request), request.getIncludeRetracted());
  }

  private FactSearchCriteria toCriteria(SearchFactRequest request) {
    return SearchFactRequestConverter.builder()
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

    public FactSearchDelegate build() {
      ObjectUtils.notNull(factSearchHandler, "Cannot instantiate FactSearchDelegate without 'factSearchHandler'.");
      return new FactSearchDelegate(factSearchHandler);
    }

    public Builder setFactSearchHandler(FactSearchHandler factSearchHandler) {
      this.factSearchHandler = factSearchHandler;
      return this;
    }
  }
}
