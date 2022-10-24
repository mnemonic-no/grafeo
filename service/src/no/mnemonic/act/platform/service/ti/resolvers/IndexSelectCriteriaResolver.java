package no.mnemonic.act.platform.service.ti.resolvers;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Resolver which creates an {@link IndexSelectCriteria} object which can be used directly in other search criterias
 * to select the indices to query.
 */
public class IndexSelectCriteriaResolver {

  static final int MAXIMUM_INDEX_RETENTION_DAYS = 1095; // Must be the same as the ILM policy configured in ElasticSearch.
  private Clock clock = Clock.systemUTC();

  /**
   * Validates 'after' and 'before' parameters provided in search endpoints and creates an {@link IndexSelectCriteria}
   * object based on those parameters. The returned criteria will select daily indices with 'indexStartTimestamp' and
   * 'indexEndTimestamp' based on 'after' and 'before'.
   * <p>
   * If the 'before' timestamp is earlier than the 'after' timestamp an {@link InvalidArgumentException} will be thrown.
   * <p>
   * If the 'after' timestamp is earlier than the index retention of 1095 days an {@link InvalidArgumentException} will be thrown.
   *
   * @param after  'after' timestamp provided by search endpoints (optionally, defaults to before - 30 days)
   * @param before 'before' timestamp provided by search endpoints (optionally, defaults to now)
   * @return Initialized {@link IndexSelectCriteria} object
   * @throws InvalidArgumentException If the 'after' and 'before' parameters fail validation
   */
  public IndexSelectCriteria validateAndCreateCriteria(Long after, Long before) throws InvalidArgumentException {
    // Specify default values for end (now) and start (end - 30 days) if those values aren't provided.
    Instant end = ObjectUtils.ifNotNull(before, Instant::ofEpochMilli, clock.instant());
    Instant start = ObjectUtils.ifNotNull(after, Instant::ofEpochMilli, end.minus(30, ChronoUnit.DAYS));

    if (end.isBefore(start)) {
      String invalidValue = String.format("after=%s|before=%s", start, end);
      throw new InvalidArgumentException()
              .addValidationError("The 'before' timestamp is earlier than the 'after' timestamp.",
                      "index.selection.invalid.timestamps", "after|before", invalidValue);
    }

    if (Duration.between(start, clock.instant()).toDays() > MAXIMUM_INDEX_RETENTION_DAYS) {
      String msg = String.format("The 'after' timestamp is earlier than the maximum index retention of %d days. Specify a later 'after' timestamp.", MAXIMUM_INDEX_RETENTION_DAYS);
      throw new InvalidArgumentException()
              .addValidationError(msg, "index.selection.invalid.after.timestamp", "after", start.toString());
    }

    return IndexSelectCriteria.builder()
            .setIndexStartTimestamp(start.toEpochMilli())
            .setIndexEndTimestamp(end.toEpochMilli())
            .build();
  }

  /* Setters used for unit testing */

  IndexSelectCriteriaResolver withClock(Clock clock) {
    this.clock = clock;
    return this;
  }
}
