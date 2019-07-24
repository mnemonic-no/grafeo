package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.request.v1.SearchMetaFactsRequest;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;

import static no.mnemonic.act.platform.dao.api.FactSearchCriteria.KeywordFieldStrategy.*;

public class SearchMetaFactsRequestConverter implements Converter<SearchMetaFactsRequest, FactSearchCriteria> {

  private static final int DEFAULT_LIMIT = 25;

  private final SecurityContext securityContext;

  @Inject
  public SearchMetaFactsRequestConverter(SecurityContext securityContext) {
    this.securityContext = securityContext;
  }

  @Override
  public Class<SearchMetaFactsRequest> getSourceType() {
    return SearchMetaFactsRequest.class;
  }

  @Override
  public Class<FactSearchCriteria> getTargetType() {
    return FactSearchCriteria.class;
  }

  @Override
  public FactSearchCriteria apply(SearchMetaFactsRequest request) {
    if (request == null) return null;
    return FactSearchCriteria.builder()
            .addInReferenceTo(request.getFact())
            .setKeywords(request.getKeywords())
            .setKeywordFieldStrategy(SetUtils.set(factValue, organization, source))
            .setFactTypeID(onlyUUID(request.getFactType()))
            .setFactTypeName(noneUUID(request.getFactType()))
            .setFactValue(request.getFactValue())
            .setOrganizationID(onlyUUID(request.getOrganization()))
            .setOrganizationName(noneUUID(request.getOrganization()))
            .setSourceID(onlyUUID(request.getSource()))
            .setSourceName(noneUUID(request.getSource()))
            .setStartTimestamp(request.getAfter())
            .setEndTimestamp(request.getBefore())
            .addTimeFieldStrategy(FactSearchCriteria.TimeFieldStrategy.timestamp)
            .setLimit(ObjectUtils.ifNull(request.getLimit(), DEFAULT_LIMIT))
            .setCurrentUserID(securityContext.getCurrentUserID())
            .setAvailableOrganizationID(securityContext.getAvailableOrganizationID())
            .build();
  }
}
