package no.mnemonic.act.platform.service.ti.resolvers;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static no.mnemonic.act.platform.service.ti.resolvers.IndexSelectCriteriaResolver.MAXIMUM_INDEX_RETENTION_DAYS;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class IndexSelectCriteriaResolverTest {

  private static final Instant NOW = Instant.parse("2022-01-01T12:00:00Z");

  @Mock
  private SecurityContext securityContext;
  @Mock
  private Clock clock;

  private IndexSelectCriteriaResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);

    when(clock.instant()).thenReturn(NOW);

    resolver = new IndexSelectCriteriaResolver(securityContext).withClock(clock);
  }

  @Test
  public void testSelectLegacyIndex() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.threatIntelUseDailyIndices);
    assertTrue(resolver.validateAndCreateCriteria(null, null).isUseLegacyIndex());
    verify(securityContext).checkPermission(TiFunctionConstants.threatIntelUseDailyIndices);
  }

  @Test
  public void testSelectDailyIndices() throws Exception {
    long start = NOW.minus(2, ChronoUnit.DAYS).toEpochMilli();
    long end = NOW.minus(1, ChronoUnit.DAYS).toEpochMilli();

    IndexSelectCriteria criteria = resolver.validateAndCreateCriteria(start, end);
    assertFalse(criteria.isUseLegacyIndex());
    assertEquals(start, criteria.getIndexStartTimestamp());
    assertEquals(end, criteria.getIndexEndTimestamp());
  }

  @Test
  public void testSelectDailyIndicesWithDefaultValuesBoth() throws Exception {
    long start = NOW.minus(30, ChronoUnit.DAYS).toEpochMilli();
    long end = NOW.toEpochMilli();

    IndexSelectCriteria criteria = resolver.validateAndCreateCriteria(null, null);
    assertFalse(criteria.isUseLegacyIndex());
    assertEquals(start, criteria.getIndexStartTimestamp());
    assertEquals(end, criteria.getIndexEndTimestamp());
  }

  @Test
  public void testSelectDailyIndicesWithDefaultValuesOnlyAfter() throws Exception {
    long start = NOW.minus(32, ChronoUnit.DAYS).toEpochMilli();
    long end = NOW.minus(2, ChronoUnit.DAYS).toEpochMilli();

    IndexSelectCriteria criteria = resolver.validateAndCreateCriteria(null, end);
    assertFalse(criteria.isUseLegacyIndex());
    assertEquals(start, criteria.getIndexStartTimestamp());
    assertEquals(end, criteria.getIndexEndTimestamp());
  }

  @Test
  public void testSelectDailyIndicesWithDefaultValuesOnlyBefore() throws Exception {
    long start = NOW.minus(7, ChronoUnit.DAYS).toEpochMilli();
    long end = NOW.toEpochMilli();

    IndexSelectCriteria criteria = resolver.validateAndCreateCriteria(start, null);
    assertFalse(criteria.isUseLegacyIndex());
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
