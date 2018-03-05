package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.request.v1.SearchFactRequest;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class SearchFactRequestConverter implements Converter<SearchFactRequest, FactSearchCriteria> {

  private static final int DEFAULT_LIMIT = 25;

  private final Supplier<UUID> currentUserIdSupplier;
  private final Supplier<Set<UUID>> availableOrganizationIdSupplier;

  private SearchFactRequestConverter(Supplier<UUID> currentUserIdSupplier, Supplier<Set<UUID>> availableOrganizationIdSupplier) {
    this.currentUserIdSupplier = currentUserIdSupplier;
    this.availableOrganizationIdSupplier = availableOrganizationIdSupplier;
  }

  @Override
  public Class<SearchFactRequest> getSourceType() {
    return SearchFactRequest.class;
  }

  @Override
  public Class<FactSearchCriteria> getTargetType() {
    return FactSearchCriteria.class;
  }

  @Override
  public FactSearchCriteria apply(SearchFactRequest request) {
    if (request == null) return null;
    return FactSearchCriteria.builder()
            .setKeywords(request.getKeywords())
            .setObjectTypeID(onlyUUID(request.getObjectType()))
            .setObjectTypeName(noneUUID(request.getObjectType()))
            .setFactTypeID(onlyUUID(request.getFactType()))
            .setFactTypeName(noneUUID(request.getFactType()))
            .setObjectValue(request.getObjectValue())
            .setFactValue(request.getFactValue())
            .setOrganizationID(onlyUUID(request.getOrganization()))
            .setOrganizationName(noneUUID(request.getOrganization()))
            .setSourceID(onlyUUID(request.getSource()))
            .setSourceName(noneUUID(request.getSource()))
            .setRetracted(determineRetracted(request.getIncludeRetracted()))
            .setStartTimestamp(request.getAfter())
            .setEndTimestamp(request.getBefore())
            .addTimeFieldStrategy(FactSearchCriteria.TimeFieldStrategy.timestamp)
            .setLimit(ObjectUtils.ifNull(request.getLimit(), DEFAULT_LIMIT))
            .setCurrentUserID(currentUserIdSupplier.get())
            .setAvailableOrganizationID(availableOrganizationIdSupplier.get())
            .build();
  }

  private Boolean determineRetracted(Boolean includeRetracted) {
    // By default don't return retracted Facts, i.e. filter Facts where 'retracted' flag is set to false.
    if (includeRetracted == null || !includeRetracted) return false;
    // Don't filter on 'retracted' flag in order to also include retracted Facts.
    // If this would return true only retracted Facts would be returned.
    return null;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Supplier<UUID> currentUserIdSupplier;
    private Supplier<Set<UUID>> availableOrganizationIdSupplier;

    private Builder() {
    }

    public SearchFactRequestConverter build() {
      ObjectUtils.notNull(currentUserIdSupplier, "Cannot instantiate SearchFactRequestConverter without 'currentUserIdSupplier'.");
      ObjectUtils.notNull(availableOrganizationIdSupplier, "Cannot instantiate SearchFactRequestConverter without 'availableOrganizationIdSupplier'.");
      return new SearchFactRequestConverter(currentUserIdSupplier, availableOrganizationIdSupplier);
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
