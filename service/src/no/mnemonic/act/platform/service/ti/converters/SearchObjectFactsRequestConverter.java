package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.request.v1.SearchObjectFactsRequest;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static no.mnemonic.act.platform.dao.api.FactSearchCriteria.KeywordFieldStrategy.*;

public class SearchObjectFactsRequestConverter implements Converter<SearchObjectFactsRequest, FactSearchCriteria> {

  private static final int DEFAULT_LIMIT = 25;

  private final Supplier<UUID> currentUserIdSupplier;
  private final Supplier<Set<UUID>> availableOrganizationIdSupplier;

  private SearchObjectFactsRequestConverter(Supplier<UUID> currentUserIdSupplier, Supplier<Set<UUID>> availableOrganizationIdSupplier) {
    this.currentUserIdSupplier = currentUserIdSupplier;
    this.availableOrganizationIdSupplier = availableOrganizationIdSupplier;
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
            .setCurrentUserID(currentUserIdSupplier.get())
            .setAvailableOrganizationID(availableOrganizationIdSupplier.get())
            .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Supplier<UUID> currentUserIdSupplier;
    private Supplier<Set<UUID>> availableOrganizationIdSupplier;

    private Builder() {
    }

    public SearchObjectFactsRequestConverter build() {
      ObjectUtils.notNull(currentUserIdSupplier, "Cannot instantiate SearchObjectFactsRequestConverter without 'currentUserIdSupplier'.");
      ObjectUtils.notNull(availableOrganizationIdSupplier, "Cannot instantiate SearchObjectFactsRequestConverter without 'availableOrganizationIdSupplier'.");
      return new SearchObjectFactsRequestConverter(currentUserIdSupplier, availableOrganizationIdSupplier);
    }

    public Builder setCurrentUserIdSupplier(Supplier<UUID> currentUserIdSupplier) {
      this.currentUserIdSupplier = currentUserIdSupplier;
      return this;
    }

    public Builder setAvailableOrganizationIdSupplier(Supplier<Set<UUID>> availableOrganizationIdSupplier) {
      this.availableOrganizationIdSupplier = availableOrganizationIdSupplier;
      return this;
    }
  }

}
