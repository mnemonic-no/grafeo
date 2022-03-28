package no.mnemonic.act.platform.dao.elastic;

import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import org.junit.Test;

import java.time.Instant;
import java.util.UUID;

import static no.mnemonic.act.platform.dao.elastic.DocumentTestUtils.assertFactDocument;
import static no.mnemonic.act.platform.dao.elastic.DocumentTestUtils.createFactDocument;
import static no.mnemonic.act.platform.dao.elastic.FactSearchManager.TargetIndex.*;
import static org.junit.Assert.*;

public class FactSearchManagerIndexFactsTest extends AbstractManagerTest {

  @Test
  public void testGetFactNullId() {
    assertNull(getFactSearchManager().getFact(null, Legacy.getName()));
  }

  @Test
  public void testGetFactNonIndexedFact() {
    getFactSearchManager().indexFact(createFactDocument(), Legacy);
    assertNull(getFactSearchManager().getFact(UUID.randomUUID(), Legacy.getName()));
  }

  @Test
  public void testIndexFactNullFact() {
    assertNull(getFactSearchManager().indexFact(null, Legacy));
  }

  @Test
  public void testIndexFactEmptyFact() {
    assertNull(getFactSearchManager().indexFact(new FactDocument(), Legacy));
  }

  @Test
  public void testIndexAndGetFact() {
    FactDocument fact = createFactDocument();

    FactDocument indexedFact = getFactSearchManager().indexFact(fact, Legacy);
    assertNotNull(indexedFact);
    assertSame(fact, indexedFact);
    assertFactDocument(fact, indexedFact);

    FactDocument fetchedFact = getFactSearchManager().getFact(fact.getId(), Legacy.getName());
    assertNotNull(fetchedFact);
    assertNotSame(fact, fetchedFact);
    assertFactDocument(fact, fetchedFact);
  }

  @Test
  public void testReindexAndGetFact() {
    FactDocument fact = createFactDocument();

    getFactSearchManager().indexFact(fact.setValue("originalValue"), Legacy);
    FactDocument indexedFact1 = getFactSearchManager().getFact(fact.getId(), Legacy.getName());
    assertEquals(fact.getId(), indexedFact1.getId());
    assertEquals("originalValue", indexedFact1.getValue());

    getFactSearchManager().indexFact(fact.setValue("updatedValue"), Legacy);
    FactDocument indexedFact2 = getFactSearchManager().getFact(fact.getId(), Legacy.getName());
    assertEquals(fact.getId(), indexedFact2.getId());
    assertEquals("updatedValue", indexedFact2.getValue());
  }

  @Test
  public void testIndexAndGetFactWithDefaultValues() {
    FactDocument fact = new FactDocument()
            .setId(UUID.randomUUID());

    getFactSearchManager().indexFact(fact, Legacy);
    FactDocument indexedFact = getFactSearchManager().getFact(fact.getId(), Legacy.getName());
    assertEquals(FactDocument.DEFAULT_CONFIDENCE, indexedFact.getConfidence(), 0);
    assertEquals(FactDocument.DEFAULT_TRUST, indexedFact.getTrust(), 0);
  }

  @Test
  public void testIndexIntoTimeGlobalIndex() {
    FactDocument fact = createFactDocument();
    getFactSearchManager().indexFact(fact, TimeGlobal);

    FactDocument fetchedFact = getFactSearchManager().getFact(fact.getId(), TimeGlobal.getName());
    assertNotNull(fetchedFact);
    assertFactDocument(fact, fetchedFact);
  }

  @Test
  public void testIndexIntoDailyIndex() {
    Instant lastSeen = Instant.parse("2022-03-22T13:13:13Z");

    FactDocument fact = createFactDocument()
            .setLastSeenTimestamp(lastSeen.toEpochMilli());
    getFactSearchManager().indexFact(fact, Daily);

    FactDocument fetchedFact = getFactSearchManager().getFact(fact.getId(), Daily.getName() + "2022-03-22");
    assertNotNull(fetchedFact);
    assertFactDocument(fact, fetchedFact);
  }

}
