package no.mnemonic.services.grafeo.service.ti.resolvers;

import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.dao.api.criteria.IndexSelectCriteria;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static no.mnemonic.services.grafeo.service.ti.resolvers.IndexSelectCriteriaResolver.MAXIMUM_INDEX_RETENTION_DAYS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class IndexSelectCriteriaResolverTest {

  private static final Instant NOW = Instant.parse("2022-01-01T12:00:00Z");

  @Mock
  private Clock clock;

  private IndexSelectCriteriaResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);

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
