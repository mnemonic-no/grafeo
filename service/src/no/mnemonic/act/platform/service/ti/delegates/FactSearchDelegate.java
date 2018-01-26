package no.mnemonic.act.platform.service.ti.delegates;

import com.google.common.collect.Streams;
import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.request.v1.SearchFactRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.SearchResult;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.SearchFactRequestConverter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FactSearchDelegate extends AbstractDelegate {

  public static FactSearchDelegate create() {
    return new FactSearchDelegate();
  }

  public ResultSet<Fact> handle(SearchFactRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    TiSecurityContext.get().checkPermission(TiFunctionConstants.viewFactObjects);

    // Search for Facts in ElasticSearch and pick out all Fact IDs.
    SearchResult<FactDocument> searchResult = TiRequestContext.get().getFactSearchManager().searchFacts(toCriteria(request));
    List<UUID> factID = searchResult.getValues().stream()
            .map(FactDocument::getId)
            .collect(Collectors.toList());

    // Use the Fact IDs to look up the authoritative data in Cassandra,
    // and make sure that a user has access to all returned Facts.
    List<Fact> facts = Streams.stream(TiRequestContext.get().getFactManager().getFacts(factID))
            .filter(fact -> TiSecurityContext.get().hasReadPermission(fact))
            .map(TiRequestContext.get().getFactConverter())
            .collect(Collectors.toList());

    return ResultSet.builder()
            .setCount(searchResult.getCount())
            .setLimit(searchResult.getLimit())
            .setValues(facts)
            .build();
  }

  private FactSearchCriteria toCriteria(SearchFactRequest request) {
    return SearchFactRequestConverter.builder()
            .setCurrentUserIdSupplier(() -> TiSecurityContext.get().getCurrentUserID())
            .setAvailableOrganizationIdSupplier(() -> TiSecurityContext.get().getAvailableOrganizationID())
            .build()
            .apply(request);
  }

}
