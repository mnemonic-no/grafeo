package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class SearchObjectRequestConverter implements Converter<SearchObjectRequest, FactSearchCriteria> {

  private static final int DEFAULT_LIMIT = 25;

  private final Supplier<UUID> currentUserIdSupplier;
  private final Supplier<Set<UUID>> availableOrganizationIdSupplier;

  private SearchObjectRequestConverter(Supplier<UUID> currentUserIdSupplier, Supplier<Set<UUID>> availableOrganizationIdSupplier) {
    this.currentUserIdSupplier = currentUserIdSupplier;
    this.availableOrganizationIdSupplier = availableOrganizationIdSupplier;
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

    public SearchObjectRequestConverter build() {
      ObjectUtils.notNull(currentUserIdSupplier, "Cannot instantiate SearchObjectRequestConverter without 'currentUserIdSupplier'.");
      ObjectUtils.notNull(availableOrganizationIdSupplier, "Cannot instantiate SearchObjectRequestConverter without 'availableOrganizationIdSupplier'.");
      return new SearchObjectRequestConverter(currentUserIdSupplier, availableOrganizationIdSupplier);
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
