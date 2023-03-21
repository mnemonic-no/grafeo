package no.mnemonic.services.grafeo.dao.elastic.helpers;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities for dealing with daily index names.
 */
public class DailyIndexNamesGenerator {

  private DailyIndexNamesGenerator() {
  }

  /**
   * Generate daily index names for a given time period. The method produces one index name for every day between
   * 'indexStartTimestamp' and 'indexEndTimestamp' (including both start and end).
   * <p>
   * Note that this method will generate one index name for every day between 'indexStartTimestamp' and 'indexEndTimestamp'.
   * It does NOT generate index wildcards. If the number of index names becomes too large to handle for ElasticSearch
   * consider increasing the 'http.max_initial_line_length' limit on the ElasticSearch cluster.
   *
   * @param indexStartTimestamp First daily index
   * @param indexEndTimestamp   Last daily index
   * @param indexNamePrefix     Prefix used for creating index names
   * @return Daily index names
   */
  public static List<String> generateIndexNames(long indexStartTimestamp, long indexEndTimestamp, String indexNamePrefix) {
    if (indexEndTimestamp < indexStartTimestamp)
      throw new IllegalArgumentException("'indexEndTimestamp' cannot be before 'indexStartTimestamp'!");

    // Truncate 'start' and 'end' to day resolution to avoid any ambiguities due to hours, minutes, seconds, etc.
    Instant start = Instant.ofEpochMilli(indexStartTimestamp).truncatedTo(ChronoUnit.DAYS);
    Instant end = Instant.ofEpochMilli(indexEndTimestamp).truncatedTo(ChronoUnit.DAYS);
    // Add one day to 'end' in order to calculate the correct amount of days between 'start' and 'end'.
    // This is needed because the second parameter of between() is excluded in the calculation.
    long numberOfDays = Duration.between(start, end.plus(1, ChronoUnit.DAYS)).toDays();

    return Stream.iterate(start, current -> current.plus(1, ChronoUnit.DAYS))
            .map(current -> formatIndexName(current.toEpochMilli(), indexNamePrefix))
            .limit(numberOfDays)
            .collect(Collectors.toList());
  }

  /**
   * Format an index name given a timestamp and prefix, e.g. 'prefix-2022-01-01'.
   *
   * @param timestamp       Timestamp representing the daily index
   * @param indexNamePrefix Prefix used for creating index name
   * @return Formatted index name
   */
  public static String formatIndexName(long timestamp, String indexNamePrefix) {
    if (indexNamePrefix == null) throw new IllegalArgumentException("'indexNamePrefix' cannot be null!");

    return indexNamePrefix + DateTimeFormatter.ISO_LOCAL_DATE
            // Need to explicitly specify time zone when formatting an Instant.
            .withZone(ZoneId.from(ZoneOffset.UTC))
            .format(Instant.ofEpochMilli(timestamp));
  }
}
