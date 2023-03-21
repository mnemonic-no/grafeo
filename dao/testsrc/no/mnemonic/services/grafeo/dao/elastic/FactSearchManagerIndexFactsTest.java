package no.mnemonic.services.grafeo.dao.elastic;

import no.mnemonic.services.grafeo.dao.elastic.document.FactDocument;
import org.junit.Test;

import java.time.Instant;
import java.util.UUID;

import static no.mnemonic.services.grafeo.dao.elastic.DocumentTestUtils.assertFactDocument;
import static no.mnemonic.services.grafeo.dao.elastic.DocumentTestUtils.createFactDocument;
import static no.mnemonic.services.grafeo.dao.elastic.FactSearchManager.TargetIndex.Daily;
import static no.mnemonic.services.grafeo.dao.elastic.FactSearchManager.TargetIndex.TimeGlobal;
import static org.junit.Assert.*;

public class FactSearchManagerIndexFactsTest extends AbstractManagerTest {

  @Test
  public void testGetFactNullId() {
    assertNull(getFactSearchManager().getFact(null, TimeGlobal.getName()));
  }

  @Test
  public void testGetFactNonIndexedFact() {
    getFactSearchManager().indexFact(createFactDocument(DAY1), TimeGlobal);
    assertNull(getFactSearchManager().getFact(UUID.randomUUID(), TimeGlobal.getName()));
  }

  @Test
  public void testIndexFactNullFact() {
    assertNull(getFactSearchManager().indexFact(null, TimeGlobal));
  }

  @Test
  public void testIndexFactEmptyFact() {
    assertNull(getFactSearchManager().indexFact(new FactDocument(), TimeGlobal));
  }

  @Test
  public void testIndexAndGetFact() {
    FactDocument fact = createFactDocument(DAY1);

    FactDocument indexedFact = getFactSearchManager().indexFact(fact, TimeGlobal);
    assertNotNull(indexedFact);
    assertSame(fact, indexedFact);
    assertFactDocument(fact, indexedFact);

    FactDocument fetchedFact = getFactSearchManager().getFact(fact.getId(), TimeGlobal.getName());
    assertNotNull(fetchedFact);
    assertNotSame(fact, fetchedFact);
    assertFactDocument(fact, fetchedFact);
  }

  @Test
  public void testReindexAndGetFact() {
    FactDocument fact = createFactDocument(DAY1);

    getFactSearchManager().indexFact(fact.setValue("originalValue"), TimeGlobal);
    FactDocument indexedFact1 = getFactSearchManager().getFact(fact.getId(), TimeGlobal.getName());
    assertEquals(fact.getId(), indexedFact1.getId());
    assertEquals("originalValue", indexedFact1.getValue());

    getFactSearchManager().indexFact(fact.setValue("updatedValue"), TimeGlobal);
    FactDocument indexedFact2 = getFactSearchManager().getFact(fact.getId(), TimeGlobal.getName());
    assertEquals(fact.getId(), indexedFact2.getId());
    assertEquals("updatedValue", indexedFact2.getValue());
  }

  @Test
  public void testIndexAndGetFactWithDefaultValues() {
    FactDocument fact = new FactDocument()
            .setId(UUID.randomUUID());

    getFactSearchManager().indexFact(fact, TimeGlobal);
    FactDocument indexedFact = getFactSearchManager().getFact(fact.getId(), TimeGlobal.getName());
    assertEquals(FactDocument.DEFAULT_CONFIDENCE, indexedFact.getConfidence(), 0);
    assertEquals(FactDocument.DEFAULT_TRUST, indexedFact.getTrust(), 0);
  }

  @Test
  public void testIndexIntoDailyIndex() {
    Instant lastSeen = Instant.parse("2022-03-22T13:13:13Z");

    FactDocument fact = createFactDocument(lastSeen.toEpochMilli());
    getFactSearchManager().indexFact(fact, Daily);

    FactDocument fetchedFact = getFactSearchManager().getFact(fact.getId(), Daily.getName() + "2022-03-22");
    assertNotNull(fetchedFact);
    assertFactDocument(fact, fetchedFact);
  }

}
