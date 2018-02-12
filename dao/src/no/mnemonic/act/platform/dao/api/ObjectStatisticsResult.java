package no.mnemonic.act.platform.dao.api;

import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.*;

/**
 * Result container holding the calculated statistics about Facts bound to Objects.
 */
public class ObjectStatisticsResult {

  private final Map<UUID, Collection<FactStatistic>> objectStatisticsMap;

  private ObjectStatisticsResult(Map<UUID, Collection<FactStatistic>> objectStatisticsMap) {
    this.objectStatisticsMap = ObjectUtils.ifNotNull(objectStatisticsMap, Collections::unmodifiableMap, Collections.emptyMap());
  }

  /**
   * Fetch the statistics about Facts for a specific Object (identified by its UUID).
   * <p>
   * Each element in the returned collection contains the statistics for one FactType, see {@link FactStatistic}
   * for the content of the returned statistics. Returns an empty collection if no statistics are available.
   *
   * @param objectID UUID of Object
   * @return Statistics about Facts bound to the specified Object
   */
  public Collection<FactStatistic> getStatistics(UUID objectID) {
    return objectStatisticsMap.getOrDefault(objectID, Collections.emptySet());
  }

  /**
   * Fetch the total number of available statistics, i.e. the number of Objects for which statistics are available.
   *
   * @return Total number of available statistics
   */
  public int getStatisticsCount() {
    return objectStatisticsMap.size();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final Map<UUID, Collection<FactStatistic>> objectStatisticsMap = new HashMap<>();

    private Builder() {
    }

    public ObjectStatisticsResult build() {
      return new ObjectStatisticsResult(objectStatisticsMap);
    }

    public Builder addStatistic(UUID objectID, FactStatistic statistic) {
      this.objectStatisticsMap.computeIfAbsent(objectID, id -> new HashSet<>()).add(statistic);
      return this;
    }
  }

  /**
   * Holds the statistics for one particular FactType.
   */
  public static class FactStatistic {
    private final UUID factTypeID;
    private final int factCount;
    private final long lastAddedTimestamp;
    private final long lastSeenTimestamp;

    public FactStatistic(UUID factTypeID, int factCount, long lastAddedTimestamp, long lastSeenTimestamp) {
      this.factTypeID = factTypeID;
      this.factCount = factCount;
      this.lastAddedTimestamp = lastAddedTimestamp;
      this.lastSeenTimestamp = lastSeenTimestamp;
    }

    /**
     * Returns the UUID of the FactType.
     *
     * @return UUID of FactType
     */
    public UUID getFactTypeID() {
      return factTypeID;
    }

    /**
     * Returns the number of Facts of this FactType bound to a specific Object.
     *
     * @return Number of Facts
     */
    public int getFactCount() {
      return factCount;
    }

    /**
     * Returns the timestamp when a Fact of this FactType was last added to a specific Object.
     *
     * @return Timestamp of Fact added last to a specific Object
     */
    public long getLastAddedTimestamp() {
      return lastAddedTimestamp;
    }

    /**
     * Returns the timestamp when a Fact of this FactType was last seen for a specific Object.
     *
     * @return Timestamp of Fact seen last for a specific Object
     */
    public long getLastSeenTimestamp() {
      return lastSeenTimestamp;
    }
  }
}
