package no.mnemonic.services.grafeo.service.implementation.converters.request;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.services.grafeo.service.implementation.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.IndexSelectCriteriaResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.SearchByNameRequestResolver;

import javax.inject.Inject;

import static no.mnemonic.services.grafeo.service.implementation.converters.request.RequestConverterUtils.handleTimeFieldSearchRequest;

public class SearchObjectFactsRequestConverter {

  private static final int DEFAULT_LIMIT = 25;

  private final SearchByNameRequestResolver byNameResolver;
  private final AccessControlCriteriaResolver accessControlCriteriaResolver;
  private final IndexSelectCriteriaResolver indexSelectCriteriaResolver;

  @Inject
  public SearchObjectFactsRequestConverter(SearchByNameRequestResolver byNameResolver,
                                           AccessControlCriteriaResolver accessControlCriteriaResolver,
                                           IndexSelectCriteriaResolver indexSelectCriteriaResolver) {
    this.byNameResolver = byNameResolver;
    this.accessControlCriteriaResolver = accessControlCriteriaResolver;
    this.indexSelectCriteriaResolver = indexSelectCriteriaResolver;
  }

  public FactSearchCriteria apply(SearchObjectFactsRequest request) throws InvalidArgumentException {
    if (request == null) return null;
    return handleTimeFieldSearchRequest(FactSearchCriteria.builder(), request)
            .addObjectID(request.getObjectID())
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
            .setLimit(ObjectUtils.ifNull(request.getLimit(), DEFAULT_LIMIT))
            .setAccessControlCriteria(accessControlCriteriaResolver.get())
            .setIndexSelectCriteria(indexSelectCriteriaResolver.validateAndCreateCriteria(request.getStartTimestamp(), request.getEndTimestamp()))
            .build();
  }
}
