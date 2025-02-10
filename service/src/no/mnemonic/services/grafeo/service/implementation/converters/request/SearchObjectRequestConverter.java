package no.mnemonic.services.grafeo.service.implementation.converters.request;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.request.v1.SearchObjectRequest;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.services.grafeo.service.implementation.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.IndexSelectCriteriaResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.SearchByNameRequestResolver;

import jakarta.inject.Inject;

import static no.mnemonic.services.grafeo.service.implementation.converters.request.RequestConverterUtils.handleTimeFieldSearchRequest;

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
