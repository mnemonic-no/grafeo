package no.mnemonic.act.platform.service.ti.converters.request;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.v1.SearchMetaFactsRequest;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.service.ti.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.act.platform.service.ti.resolvers.IndexSelectCriteriaResolver;
import no.mnemonic.act.platform.service.ti.resolvers.request.SearchByNameRequestResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;

public class SearchMetaFactsRequestConverter {

  private static final int DEFAULT_LIMIT = 25;

  private final SearchByNameRequestResolver byNameResolver;
  private final AccessControlCriteriaResolver accessControlCriteriaResolver;
  private final IndexSelectCriteriaResolver indexSelectCriteriaResolver;

  @Inject
  public SearchMetaFactsRequestConverter(SearchByNameRequestResolver byNameResolver,
                                         AccessControlCriteriaResolver accessControlCriteriaResolver,
                                         IndexSelectCriteriaResolver indexSelectCriteriaResolver) {
    this.byNameResolver = byNameResolver;
    this.accessControlCriteriaResolver = accessControlCriteriaResolver;
    this.indexSelectCriteriaResolver = indexSelectCriteriaResolver;
  }

  public FactSearchCriteria apply(SearchMetaFactsRequest request) throws InvalidArgumentException {
    if (request == null) return null;
    return FactSearchCriteria.builder()
            .addInReferenceTo(request.getFact())
            .setKeywords(request.getKeywords())
            .setKeywordFieldStrategy(SetUtils.set(
                    FactSearchCriteria.KeywordFieldStrategy.factValueText,
                    FactSearchCriteria.KeywordFieldStrategy.factValueIp,
                    FactSearchCriteria.KeywordFieldStrategy.factValueDomain))
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
            .addTimeFieldStrategy(FactSearchCriteria.TimeFieldStrategy.lastSeenTimestamp)
            .setLimit(ObjectUtils.ifNull(request.getLimit(), DEFAULT_LIMIT))
            .setAccessControlCriteria(accessControlCriteriaResolver.get())
            .setIndexSelectCriteria(indexSelectCriteriaResolver.validateAndCreateCriteria(request.getAfter(), request.getBefore()))
            .build();
  }
}
