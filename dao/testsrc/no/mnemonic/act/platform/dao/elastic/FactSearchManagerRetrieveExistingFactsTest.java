package no.mnemonic.act.platform.dao.elastic;

import no.mnemonic.act.platform.dao.api.FactExistenceSearchCriteria;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.dao.elastic.document.SearchResult;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.elastic.DocumentTestUtils.assertFactDocument;
import static no.mnemonic.act.platform.dao.elastic.DocumentTestUtils.createObjectDocument;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FactSearchManagerRetrieveExistingFactsTest extends AbstractManagerTest {

  @Test
  public void testRetrieveExistingFactsWithNoCriteria() {
    assertNotNull(getFactSearchManager().retrieveExistingFacts(null));
  }

  @Test
  public void testRetrieveExistingFactsReturnsExisting() {
    FactDocument fact = indexFact(d -> d);
    SearchResult<FactDocument> result = getFactSearchManager().retrieveExistingFacts(createCriteriaWithObjects(fact, b -> b));
    assertEquals(1, result.getCount());
    assertEquals(1, result.getValues().size());
    assertFactDocument(fact, result.getValues().get(0));
  }

  @Test
  public void testRetrieveExistingFactsMatchesOnNullFactValue() {
    FactDocument fact = indexFact(d -> d.setValue(null));
    SearchResult<FactDocument> result = getFactSearchManager().retrieveExistingFacts(createCriteriaWithObjects(fact, b -> b.setFactValue(null)));
    assertEquals(1, result.getCount());
    assertEquals(1, result.getValues().size());
    assertFactDocument(fact, result.getValues().get(0));
  }

  @Test
  public void testRetrieveExistingFactsNoMatchOnFactValue() {
    testRetrieveExistingFactsNoMatch(b -> b.setFactValue("something"));
  }

  @Test
  public void testRetrieveExistingFactsNoMatchOnNullFactValue() {
    testRetrieveExistingFactsNoMatch(b -> b.setFactValue(null));
  }

  @Test
  public void testRetrieveExistingFactsNoMatchOnFactType() {
    testRetrieveExistingFactsNoMatch(b -> b.setFactTypeID(UUID.randomUUID()));
  }

  @Test
  public void testRetrieveExistingFactsNoMatchOnSource() {
    testRetrieveExistingFactsNoMatch(b -> b.setSourceID(UUID.randomUUID()));
  }

  @Test
  public void testRetrieveExistingFactsNoMatchOnOrganization() {
    testRetrieveExistingFactsNoMatch(b -> b.setOrganizationID(UUID.randomUUID()));
  }

  @Test
  public void testRetrieveExistingFactsNoMatchOnAccessMode() {
    testRetrieveExistingFactsNoMatch(b -> b.setAccessMode("Explicit"));
  }

  @Test
  public void testRetrieveExistingFactsNoMatchOnConfidence() {
    testRetrieveExistingFactsNoMatch(b -> b.setConfidence(0.9f));
  }

  @Test
  public void testRetrieveExistingFactsNoMatchOnObjectID() {
    FactDocument fact = indexFact(d -> d);
    FactExistenceSearchCriteria criteria = createCriteriaWithoutObjects(fact, b -> {
      ObjectDocument object = first(fact.getObjects());
      return b.addObject(UUID.randomUUID(), object.getDirection().name());
    });
    testRetrieveExistingFactsNoMatch(criteria);
  }

  @Test
  public void testRetrieveExistingFactsNoMatchOnDirection() {
    FactDocument fact = indexFact(d -> d);
    FactExistenceSearchCriteria criteria = createCriteriaWithoutObjects(fact, b -> {
      ObjectDocument object = first(fact.getObjects());
      return b.addObject(object.getId(), "FactIsSource");
    });
    testRetrieveExistingFactsNoMatch(criteria);
  }

  @Test
  public void testRetrieveExistingFactsNoMatchOnObjectsCount() {
    FactDocument fact = indexFact(d -> d.setObjects(SetUtils.set(createObjectDocument(), createObjectDocument())));
    FactExistenceSearchCriteria criteria = createCriteriaWithoutObjects(fact, b -> {
      ObjectDocument object = first(fact.getObjects());
      return b.addObject(object.getId(), object.getDirection().name());
    });
    testRetrieveExistingFactsNoMatch(criteria);
  }

  @Test
  public void testRetrieveExistingMetaFactsReturnsExisting() {
    FactDocument fact = indexFact(d -> d);
    SearchResult<FactDocument> result = getFactSearchManager().retrieveExistingFacts(
            createCriteriaWithoutObjects(fact, b -> b.setInReferenceTo(fact.getInReferenceTo()))
    );
    assertEquals(1, result.getCount());
    assertEquals(1, result.getValues().size());
    assertFactDocument(fact, result.getValues().get(0));
  }

  @Test
  public void testRetrieveExistingMetaFactsNoMatchOnInReferenceTo() {
    FactDocument fact = indexFact(d -> d);
    testRetrieveExistingFactsNoMatch(createCriteriaWithoutObjects(fact, b -> b.setInReferenceTo(UUID.randomUUID())));
  }

  private void testRetrieveExistingFactsNoMatch(ObjectPreparation<FactExistenceSearchCriteria.Builder> criteriaPreparation) {
    FactDocument fact = indexFact(d -> d);
    FactExistenceSearchCriteria criteria = createCriteriaWithObjects(fact, criteriaPreparation);
    testRetrieveExistingFactsNoMatch(criteria);
  }

  private void testRetrieveExistingFactsNoMatch(FactExistenceSearchCriteria criteria) {
    SearchResult<FactDocument> result = getFactSearchManager().retrieveExistingFacts(criteria);
    assertEquals(0, result.getCount());
    assertEquals(0, result.getValues().size());
  }

  private FactExistenceSearchCriteria createCriteriaWithObjects(FactDocument fact, ObjectPreparation<FactExistenceSearchCriteria.Builder> preparation) {
    return createCriteriaWithoutObjects(fact, builder -> {
      for (ObjectDocument object : fact.getObjects()) {
        builder = builder.addObject(object.getId(), object.getDirection().name());
      }

      if (preparation != null) {
        builder = preparation.prepare(builder);
      }

      return builder;
    });
  }

  private FactExistenceSearchCriteria createCriteriaWithoutObjects(FactDocument fact, ObjectPreparation<FactExistenceSearchCriteria.Builder> preparation) {
    FactExistenceSearchCriteria.Builder builder = FactExistenceSearchCriteria.builder()
            .setFactValue(fact.getValue())
            .setFactTypeID(fact.getTypeID())
            .setSourceID(fact.getSourceID())
            .setOrganizationID(fact.getOrganizationID())
            .setAccessMode(fact.getAccessMode().name())
            .setConfidence(fact.getConfidence());

    if (preparation != null) {
      builder = preparation.prepare(builder);
    }

    return builder.build();
  }

}
