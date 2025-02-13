package no.mnemonic.services.grafeo.service.implementation.resolvers;

import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.dao.api.criteria.IndexSelectCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static no.mnemonic.services.grafeo.service.implementation.resolvers.IndexSelectCriteriaResolver.MAXIMUM_INDEX_RETENTION_DAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IndexSelectCriteriaResolverTest {

  private static final Instant NOW = Instant.parse("2022-01-01T12:00:00Z");

  @Mock
  private Clock clock;

  private IndexSelectCriteriaResolver resolver;

  @BeforeEach
  public void setUp() {
    when(clock.instant()).thenReturn(NOW);

    resolver = new IndexSelectCriteriaResolver().withClock(clock);
  }

  @Test
  public void testSelectDailyIndices() throws Exception {
    long start = NOW.minus(2, ChronoUnit.DAYS).toEpochMilli();
    long end = NOW.minus(1, ChronoUnit.DAYS).toEpochMilli();

    IndexSelectCriteria criteria = resolver.validateAndCreateCriteria(start, end);
    assertEquals(start, criteria.getIndexStartTimestamp());
    assertEquals(end, criteria.getIndexEndTimestamp());
  }

  @Test
  public void testSelectDailyIndicesWithDefaultValuesBoth() throws Exception {
    long start = NOW.minus(30, ChronoUnit.DAYS).toEpochMilli();
    long end = NOW.toEpochMilli();

    IndexSelectCriteria criteria = resolver.validateAndCreateCriteria(null, null);
    assertEquals(start, criteria.getIndexStartTimestamp());
    assertEquals(end, criteria.getIndexEndTimestamp());
  }

  @Test
  public void testSelectDailyIndicesWithDefaultValuesOnlyAfter() throws Exception {
    long start = NOW.minus(32, ChronoUnit.DAYS).toEpochMilli();
    long end = NOW.minus(2, ChronoUnit.DAYS).toEpochMilli();

    IndexSelectCriteria criteria = resolver.validateAndCreateCriteria(null, end);
    assertEquals(start, criteria.getIndexStartTimestamp());
    assertEquals(end, criteria.getIndexEndTimestamp());
  }

  @Test
  public void testSelectDailyIndicesWithDefaultValuesOnlyBefore() throws Exception {
    long start = NOW.minus(7, ChronoUnit.DAYS).toEpochMilli();
    long end = NOW.toEpochMilli();

    IndexSelectCriteria criteria = resolver.validateAndCreateCriteria(start, null);
    assertEquals(start, criteria.getIndexStartTimestamp());
    assertEquals(end, criteria.getIndexEndTimestamp());
  }

  @Test
  public void testSelectDailyIndicesFailsOnEndBeforeStart() {
    long start = NOW.minus(1, ChronoUnit.DAYS).toEpochMilli();
    long end = NOW.minus(2, ChronoUnit.DAYS).toEpochMilli();

    assertThrows(InvalidArgumentException.class, () -> resolver.validateAndCreateCriteria(start, end));
  }

  @Test
  public void testSelectDailyIndicesFailsOnStartOlderThanRetention() {
    long start = NOW.minus(MAXIMUM_INDEX_RETENTION_DAYS + 1, ChronoUnit.DAYS).toEpochMilli();
    long end = NOW.toEpochMilli();

    assertThrows(InvalidArgumentException.class, () -> resolver.validateAndCreateCriteria(start, end));
  }
}
