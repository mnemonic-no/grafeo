package no.mnemonic.act.platform.service.ti.handlers;

import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler class computing whether a Fact has been retracted. See {@link #isRetracted(UUID, Boolean)} for the details.
 */
public class FactRetractionHandler {

  private final Map<UUID, Boolean> retractionCache = new ConcurrentHashMap<>();

  private final FactTypeResolver factTypeResolver;
  private final FactSearchManager factSearchManager;
  private final TiSecurityContext securityContext;

  private FactRetractionHandler(FactTypeResolver factTypeResolver,
                                FactSearchManager factSearchManager,
                                TiSecurityContext securityContext) {
    this.factTypeResolver = factTypeResolver;
    this.factSearchManager = factSearchManager;
    this.securityContext = securityContext;
  }

  /**
   * Check whether a Fact as been retracted (from the current user's point of view).
   *
   * @param factID ID of Fact
   * @return True if Fact has been retracted
   * @see #isRetracted(UUID, Boolean)
   */
  public boolean isRetracted(UUID factID) {
    return isRetracted(factID, null);
  }

  /**
   * Check whether a Fact as been retracted (from the current user's point of view).
   * <p>
   * A Fact has been retracted from the current user's point of view if the user has access to at least one Retraction
   * Fact and that Retraction Fact is not in turn retracted. The implementation will recursively follow the retractions
   * in order to figure out if retractions have been retracted.
   * <p>
   * The result of the above computation is cached per class instance in order to speed up multiple checks for the same
   * Fact. Note that one class instance should not been used across multiple requests from different users because the
   * computation result depends on whether the user has access to retractions.
   * <p>
   * Provide the 'retractedHint' parameter to optimize the computation. If it is known upfront that the Fact has never
   * been retracted it is not necessary to perform the computation by recursively following retractions. It is still
   * useful to call this method in order to populate the cache.
   *
   * @param factID        ID of Fact
   * @param retractedHint Set to false if the Fact has never been retracted
   * @return True if Fact has been retracted
   */
  public boolean isRetracted(UUID factID, Boolean retractedHint) {
    if (factID == null) return false;

    // If it's known that the Fact has never been retracted store this information immediately.
    // This will save a lot of calls to ElasticSearch!
    if (retractedHint != null && !retractedHint) {
      retractionCache.put(factID, false);
    }

    // If no hint is provided or the Fact has been retracted by some user
    // compute if the Fact is retracted from the current user's point of view.
    return retractionCache.computeIfAbsent(factID, this::computeRetraction);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private FactTypeResolver factTypeResolver;
    private FactSearchManager factSearchManager;
    private TiSecurityContext securityContext;

    private Builder() {
    }

    public FactRetractionHandler build() {
      ObjectUtils.notNull(factTypeResolver, "Cannot instantiate FactRetractionHandler without 'factTypeResolver'.");
      ObjectUtils.notNull(factSearchManager, "Cannot instantiate FactRetractionHandler without 'factSearchManager'.");
      ObjectUtils.notNull(securityContext, "Cannot instantiate FactRetractionHandler without 'securityContext'.");
      return new FactRetractionHandler(factTypeResolver, factSearchManager, securityContext);
    }

    public Builder setFactTypeResolver(FactTypeResolver factTypeResolver) {
      this.factTypeResolver = factTypeResolver;
      return this;
    }

    public Builder setFactSearchManager(FactSearchManager factSearchManager) {
      this.factSearchManager = factSearchManager;
      return this;
    }

    public Builder setSecurityContext(TiSecurityContext securityContext) {
      this.securityContext = securityContext;
      return this;
    }
  }

  private boolean computeRetraction(UUID factID) {
    List<FactDocument> retractions = fetchRetractions(factID);
    if (CollectionUtils.isEmpty(retractions)) {
      // No accessible retractions, thus, the Fact isn't retracted.
      return false;
    }

    // The Fact is only retracted if not all of the retractions themselves are retracted.
    return !retractions.stream().allMatch(fact -> computeRetraction(fact.getId()));
  }

  private List<FactDocument> fetchRetractions(UUID factID) {
    // Create criteria to fetch all Retraction Facts for a given Fact. Only return retractions which a user has access
    // to. No access to retractions means that from the user's perspective the referenced Fact isn't retracted.
    FactSearchCriteria retractionsCriteria = FactSearchCriteria.builder()
            .addInReferenceTo(factID)
            .addFactTypeID(factTypeResolver.resolveRetractionFactType().getId())
            .setCurrentUserID(securityContext.getCurrentUserID())
            .setAvailableOrganizationID(securityContext.getAvailableOrganizationID())
            .build();

    // The number of retractions will be very small (typically one), thus, it's no problem to consume all results at once.
    return ListUtils.list(factSearchManager.searchFacts(retractionsCriteria));
  }
}
