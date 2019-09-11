package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;

import static no.mnemonic.act.platform.dao.api.FactSearchCriteria.KeywordFieldStrategy.*;

public class SearchObjectFactsRequestConverter implements Converter<SearchObjectFactsRequest, FactSearchCriteria> {

  private static final int DEFAULT_LIMIT = 25;

  private final SecurityContext securityContext;

  @Inject
  public SearchObjectFactsRequestConverter(SecurityContext securityContext) {
    this.securityContext = securityContext;
  }

  @Override
  public Class<SearchObjectFactsRequest> getSourceType() {
    return SearchObjectFactsRequest.class;
  }

  @Override
  public Class<FactSearchCriteria> getTargetType() {
    return FactSearchCriteria.class;
  }

  @Override
  public FactSearchCriteria apply(SearchObjectFactsRequest request) {
    if (request == null) return null;
    return FactSearchCriteria.builder()
            .addObjectID(request.getObjectID())
            .addObjectTypeName(request.getObjectType())
            .addObjectValue(request.getObjectValue())
            .setKeywords(request.getKeywords())
            .setKeywordFieldStrategy(SetUtils.set(factValue, organization, origin))
            .setFactTypeID(onlyUUID(request.getFactType()))
            .setFactTypeName(noneUUID(request.getFactType()))
            .setFactValue(request.getFactValue())
            .setOrganizationID(onlyUUID(request.getOrganization()))
            .setOrganizationName(noneUUID(request.getOrganization()))
            .setOriginID(onlyUUID(request.getOrigin()))
            .setOriginName(noneUUID(request.getOrigin()))
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
