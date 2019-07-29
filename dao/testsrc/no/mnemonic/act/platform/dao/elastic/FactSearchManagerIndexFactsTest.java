package no.mnemonic.act.platform.dao.elastic;

import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import org.junit.Test;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.elastic.DocumentTestUtils.assertFactDocument;
import static no.mnemonic.act.platform.dao.elastic.DocumentTestUtils.createFactDocument;
import static org.junit.Assert.*;

public class FactSearchManagerIndexFactsTest extends AbstractManagerTest {

  @Test
  public void testGetFactNullId() {
    assertNull(getFactSearchManager().getFact(null));
  }

  @Test
  public void testGetFactNonIndexedFact() {
    assertNull(getFactSearchManager().getFact(UUID.randomUUID()));
  }

  @Test
  public void testIndexFactNullFact() {
    assertNull(getFactSearchManager().indexFact(null));
  }

  @Test
  public void testIndexFactEmptyFact() {
    assertNull(getFactSearchManager().indexFact(new FactDocument()));
  }

  @Test
  public void testIndexAndGetFact() {
    FactDocument fact = createFactDocument();

    FactDocument indexedFact = getFactSearchManager().indexFact(fact);
    assertNotNull(indexedFact);
    assertSame(fact, indexedFact);
    assertFactDocument(fact, indexedFact);

    FactDocument fetchedFact = getFactSearchManager().getFact(fact.getId());
    assertNotNull(fetchedFact);
    assertNotSame(fact, fetchedFact);
    assertFactDocument(fact, fetchedFact);
  }

  @Test
  public void testReindexAndGetFact() {
    FactDocument fact = createFactDocument();

    getFactSearchManager().indexFact(fact.setValue("originalValue"));
    FactDocument indexedFact1 = getFactSearchManager().getFact(fact.getId());
    assertEquals(fact.getId(), indexedFact1.getId());
    assertEquals("originalValue", indexedFact1.getValue());

    getFactSearchManager().indexFact(fact.setValue("updatedValue"));
    FactDocument indexedFact2 = getFactSearchManager().getFact(fact.getId());
    assertEquals(fact.getId(), indexedFact2.getId());
    assertEquals("updatedValue", indexedFact2.getValue());
  }

  @Test
  public void testIndexAndGetFactWithDefaultValues() {
    FactDocument fact = new FactDocument()
            .setId(UUID.randomUUID());

    getFactSearchManager().indexFact(fact);
    FactDocument indexedFact = getFactSearchManager().getFact(fact.getId());
    assertEquals(FactDocument.DEFAULT_CONFIDENCE, indexedFact.getConfidence(), 0);
    assertEquals(FactDocument.DEFAULT_TRUST, indexedFact.getTrust(), 0);
  }

}
