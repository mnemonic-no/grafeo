package no.mnemonic.act.platform.service.ti.converters.request;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.service.ti.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.act.platform.service.ti.resolvers.IndexSelectCriteriaResolver;
import no.mnemonic.act.platform.service.ti.resolvers.request.SearchByNameRequestResolver;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;

import static no.mnemonic.act.platform.service.ti.converters.request.RequestConverterUtils.handleTimeFieldSearchRequest;

public class SearchObjectRequestConverter {

  private static final int DEFAULT_LIMIT = 25;

  private final SearchByNameRequestResolver byNameResolver;
  private final AccessControlCriteriaResolver accessControlCriteriaResolver;
  private final IndexSelectCriteriaResolver indexSelectCriteriaResolver;

  @Inject
  public SearchObjectRequestConverter(SearchByNameRequestResolver byNameResolver,
                                      AccessControlCriteriaResolver accessControlCriteriaResolver,
                                      IndexSelectCriteriaResolver indexSelectCriteriaResolver) {
    this.byNameResolver = byNameResolver;
    this.accessControlCriteriaResolver = accessControlCriteriaResolver;
    this.indexSelectCriteriaResolver = indexSelectCriteriaResolver;
  }

  public FactSearchCriteria apply(SearchObjectRequest request) throws InvalidArgumentException {
    if (request == null) return null;
    return handleTimeFieldSearchRequest(FactSearchCriteria.builder(), request)
            .setKeywords(request.getKeywords())
            .setObjectID(request.getObjectID())
            .setFactID(request.getFactID())
            .setObjectTypeID(byNameResolver.resolveObjectType(request.getObjectType()))
            .setFactTypeID(byNameResolver.resolveFactType(request.getFactType()))
            .setObjectValue(request.getObjectValue())
            .setFactValue(request.getFactValue())
            .setOrganizationID(byNameResolver.resolveOrganization(request.getOrganization()))
            .setOriginID(byNameResolver.resolveOrigin(request.getOrigin()))
            .setMinNumber(request.getMinimum())
            .setMaxNumber(request.getMaximum())
            .addNumberFieldStrategy(ObjectUtils.ifNotNull(request.getDimension(),
                    dimension -> FactSearchCriteria.NumberFieldStrategy.valueOf(dimension.name()),
                    FactSearchCriteria.NumberFieldStrategy.certainty))
            .setMinimumFactsCount(request.getMinimumFactsCount())
            .setMaximumFactsCount(request.getMaximumFactsCount())
            .setLimit(ObjectUtils.ifNull(request.getLimit(), DEFAULT_LIMIT))
            .setAccessControlCriteria(accessControlCriteriaResolver.get())
            .setIndexSelectCriteria(indexSelectCriteriaResolver.validateAndCreateCriteria(request.getStartTimestamp(), request.getEndTimestamp()))
            .build();
  }
}
