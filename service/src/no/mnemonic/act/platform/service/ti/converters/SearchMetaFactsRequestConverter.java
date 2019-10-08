package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.v1.SearchMetaFactsRequest;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.resolvers.SearchByNameResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;

public class SearchMetaFactsRequestConverter {

  private static final int DEFAULT_LIMIT = 25;

  private final SearchByNameResolver byNameResolver;
  private final SecurityContext securityContext;

  @Inject
  public SearchMetaFactsRequestConverter(SearchByNameResolver byNameResolver,
                                         SecurityContext securityContext) {
    this.byNameResolver = byNameResolver;
    this.securityContext = securityContext;
  }

  public FactSearchCriteria apply(SearchMetaFactsRequest request) throws InvalidArgumentException {
    if (request == null) return null;
    return FactSearchCriteria.builder()
            .addInReferenceTo(request.getFact())
            .setKeywords(request.getKeywords())
            .setKeywordFieldStrategy(SetUtils.set(FactSearchCriteria.KeywordFieldStrategy.factValue))
            .setFactTypeID(byNameResolver.resolveFactType(request.getFactType()))
            .setFactValue(request.getFactValue())
            .setOrganizationID(byNameResolver.resolveOrganization(request.getOrganization()))
            .setOriginID(byNameResolver.resolveOrigin(request.getOrigin()))
            .setMinNumber(request.getMinimum())
            .setMaxNumber(request.getMaximum())
            .addNumberFieldStrategy(ObjectUtils.ifNotNull(request.getDimension(),
                    dimension -> FactSearchCriteria.NumberFieldStrategy.valueOf(dimension.name()),
                    FactSearchCriteria.NumberFieldStrategy.certainty))
            .setStartTimestamp(request.getAfter())
            .setEndTimestamp(request.getBefore())
            .addTimeFieldStrategy(FactSearchCriteria.TimeFieldStrategy.timestamp)
            .setLimit(ObjectUtils.ifNull(request.getLimit(), DEFAULT_LIMIT))
            .setCurrentUserID(securityContext.getCurrentUserID())
            .setAvailableOrganizationID(securityContext.getAvailableOrganizationID())
            .build();
  }
}
