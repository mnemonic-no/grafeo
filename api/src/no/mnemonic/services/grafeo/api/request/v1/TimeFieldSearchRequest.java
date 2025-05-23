package no.mnemonic.services.grafeo.api.request.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.services.grafeo.utilities.json.TimestampDeserializer;

import java.util.Set;

/**
 * Common interface to implement time search which ensures a consistent REST API.
 */
public interface TimeFieldSearchRequest {

  enum TimeMatchStrategy {
    all, any
  }

  enum TimeFieldStrategy {
    all, timestamp, lastSeenTimestamp
  }

  @Schema(description = "Filter Facts by time after the given timestamp. Use timeFieldStrategy to define which " +
          "fields must match the time period", example = "2016-09-28T21:26:22Z", type = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  Long getStartTimestamp();

  @Schema(description = "Filter Facts by time before the given timestamp. Use timeFieldStrategy to define which " +
          "fields must match the time period", example = "2016-09-28T21:26:22Z", type = "string")
  @JsonDeserialize(using = TimestampDeserializer.class)
  Long getEndTimestamp();

  @Schema(description = "Specify whether all or any (default) of the fields defined by timeFieldStrategy must match " +
          "the time period indicated by startTimestamp and endTimestamp", example = "any")
  TimeMatchStrategy getTimeMatchStrategy();

  @Schema(description = "Specify which fields must match the time period indicated by startTimestamp and endTimestamp " +
          "(default 'lastSeenTimestamp')")
  Set<TimeFieldStrategy> getTimeFieldStrategy();

}
