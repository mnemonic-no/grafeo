package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;

public class SearchObjectRequestConverter implements Converter<SearchObjectRequest, FactSearchCriteria> {

  private static final int DEFAULT_LIMIT = 25;

  private final SecurityContext securityContext;

  @Inject
  public SearchObjectRequestConverter(SecurityContext securityContext) {
    this.securityContext = securityContext;
  }

  @Override
  public Class<SearchObjectRequest> getSourceType() {
    return SearchObjectRequest.class;
  }

  @Override
  public Class<FactSearchCriteria> getTargetType() {
    return FactSearchCriteria.class;
  }

  @Override
  public FactSearchCriteria apply(SearchObjectRequest request) {
    if (request == null) return null;
    return FactSearchCriteria.builder()
            .setKeywords(request.getKeywords())
            .setObjectID(request.getObjectID())
            .setFactID(request.getFactID())
            .setObjectTypeID(onlyUUID(request.getObjectType()))
            .setObjectTypeName(noneUUID(request.getObjectType()))
            .setFactTypeID(onlyUUID(request.getFactType()))
            .setFactTypeName(noneUUID(request.getFactType()))
            .setObjectValue(request.getObjectValue())
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
