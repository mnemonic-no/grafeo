package no.mnemonic.services.grafeo.dao.elastic.helpers;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.services.grafeo.dao.elastic.helpers.DailyIndexNamesGenerator.formatIndexName;
import static no.mnemonic.services.grafeo.dao.elastic.helpers.DailyIndexNamesGenerator.generateIndexNames;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DailyIndexNamesGeneratorTest {

  @Test
  public void testGenerateIndexNamesWithEndBeforeStart() {
    long start = Instant.parse("2022-01-01T13:00:00Z").toEpochMilli();
    long end = Instant.parse("2022-01-01T12:00:00Z").toEpochMilli();

    assertThrows(IllegalArgumentException.class, () -> generateIndexNames(start, end, "daily-"));
  }

  @Test
  public void testGenerateIndexNamesWithStartEqualsEnd() {
    long timestamp = Instant.parse("2022-01-01T12:00:00Z").toEpochMilli();
    assertEquals(list("daily-2022-01-01"), generateIndexNames(timestamp, timestamp, "daily-"));
  }

  @Test
  public void testGenerateIndexNamesSingleDay() {
    long start = Instant.parse("2022-01-01T12:00:00Z").toEpochMilli();
    long end = Instant.parse("2022-01-01T13:00:00Z").toEpochMilli();

    assertEquals(list("daily-2022-01-01"), generateIndexNames(start, end, "daily-"));
  }

  @Test
  public void testGenerateIndexNamesMultipleDays() {
    long start = Instant.parse("2022-01-01T12:00:00Z").toEpochMilli();
    long end = Instant.parse("2022-01-03T12:00:00Z").toEpochMilli();

    assertEquals(list("daily-2022-01-01", "daily-2022-01-02", "daily-2022-01-03"), generateIndexNames(start, end, "daily-"));
  }

  @Test
  public void testGenerateIndexNamesMultipleDaysOnDayBoundary() {
    long start = Instant.parse("2022-01-01T00:00:00Z").toEpochMilli();
    long end = Instant.parse("2022-01-03T00:00:00Z").toEpochMilli();

    assertEquals(list("daily-2022-01-01", "daily-2022-01-02", "daily-2022-01-03"), generateIndexNames(start, end, "daily-"));
  }

  @Test
  public void testGenerateIndexNamesMultipleDaysCrossingDayBoundaryAndLessThan24h() {
    long start = Instant.parse("2022-08-12T22:00:00Z").toEpochMilli();
    long end = Instant.parse("2022-08-13T21:59:00Z").toEpochMilli();

    assertEquals(list("daily-2022-08-12", "daily-2022-08-13"), generateIndexNames(start, end, "daily-"));
  }

  @Test
  public void testFormatIndexNameWithNullPrefix() {
    assertThrows(IllegalArgumentException.class, () -> formatIndexName(0, null));
  }

  @Test
  public void testFormatIndexNameWithBlankPrefix() {
    long timestamp = Instant.parse("2022-01-01T12:00:00Z").toEpochMilli();
    assertEquals("2022-01-01", formatIndexName(timestamp, ""));
  }

  @Test
  public void testFormatIndexNameWithGivenPrefix() {
    long timestamp = Instant.parse("2022-01-01T12:00:00Z").toEpochMilli();
    assertEquals("daily-2022-01-01", formatIndexName(timestamp, "daily-"));
  }
}
