package no.mnemonic.act.platform.dao.elastic;

import no.mnemonic.act.platform.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.dao.elastic.result.SearchResult;
import org.junit.Test;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.elastic.DocumentTestUtils.createObjectDocument;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FactSearchManagerSearchObjectsTest extends AbstractManagerTest {

  @Test
  public void testSearchObjectsWithNoCriteria() {
    assertNotNull(getFactSearchManager().searchObjects(null));
  }

  @Test
  public void testSearchObjectsWithoutIndices() {
    testSearchObjects(createFactSearchCriteria(b -> b), 0);
  }

  @Test
  public void testSearchObjectsAccessToOnlyPublicFact() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Public));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b);
    testSearchObjects(criteria, first(accessibleFact.getObjects()));
  }

  @Test
  public void testSearchObjectsAccessToRoleBasedFactViaOrganization() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b
            .setAccessControlCriteria(AccessControlCriteria.builder()
                    .addCurrentUserIdentity(UUID.randomUUID())
                    .addAvailableOrganizationID(accessibleFact.getOrganizationID())
                    .build()));
    testSearchObjects(criteria, first(accessibleFact.getObjects()));
  }

  @Test
  public void testSearchObjectsAccessToRoleBasedFactViaACL() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b
            .setAccessControlCriteria(AccessControlCriteria.builder()
                    .addCurrentUserIdentity(first(accessibleFact.getAcl()))
                    .addAvailableOrganizationID(UUID.randomUUID())
                    .build()));
    testSearchObjects(criteria, first(accessibleFact.getObjects()));
  }

  @Test
  public void testSearchObjectsAccessToExplicitFact() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b
            .setAccessControlCriteria(AccessControlCriteria.builder()
                    .addCurrentUserIdentity(first(accessibleFact.getAcl()))
                    .addAvailableOrganizationID(UUID.randomUUID())
                    .build()));
    testSearchObjects(criteria, first(accessibleFact.getObjects()));
  }

  @Test
  public void testSearchObjectsFilterByObjectIdFromDifferentFacts() {
    ObjectDocument accessibleObject = createObjectDocument().setId(UUID.randomUUID());
    indexFact(d -> d.setObjects(set(accessibleObject)));
    indexFact(d -> d.setObjects(set(createObjectDocument().setId(UUID.randomUUID()))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addObjectID(accessibleObject.getId()));
    testSearchObjects(criteria, accessibleObject);
  }

  @Test
  public void testSearchObjectsFilterByObjectIdFromSameFact() {
    ObjectDocument accessibleObject = createObjectDocument().setId(UUID.randomUUID());
    ObjectDocument inaccessibleObject = createObjectDocument().setId(UUID.randomUUID());
    indexFact(d -> d.setObjects(set(accessibleObject, inaccessibleObject)));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addObjectID(accessibleObject.getId()));
    testSearchObjects(criteria, accessibleObject);
  }

  @Test
  public void testSearchObjectsFilterByObjectTypeIdFromDifferentFacts() {
    ObjectDocument accessibleObject = createObjectDocument().setTypeID(UUID.randomUUID());
    indexFact(d -> d.setObjects(set(accessibleObject)));
    indexFact(d -> d.setObjects(set(createObjectDocument().setTypeID(UUID.randomUUID()))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addObjectTypeID(accessibleObject.getTypeID()));
    testSearchObjects(criteria, accessibleObject);
  }

  @Test
  public void testSearchObjectsFilterByObjectTypeIdFromSameFact() {
    ObjectDocument accessibleObject = createObjectDocument().setTypeID(UUID.randomUUID());
    ObjectDocument inaccessibleObject = createObjectDocument().setTypeID(UUID.randomUUID());
    indexFact(d -> d.setObjects(set(accessibleObject, inaccessibleObject)));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addObjectTypeID(accessibleObject.getTypeID()));
    testSearchObjects(criteria, accessibleObject);
  }

  @Test
  public void testSearchObjectsFilterByObjectValueFromDifferentFacts() {
    ObjectDocument accessibleObject = createObjectDocument().setValue("objectValueA");
    indexFact(d -> d.setObjects(set(accessibleObject)));
    indexFact(d -> d.setObjects(set(createObjectDocument().setValue("objectValueB"))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addObjectValue(accessibleObject.getValue()));
    testSearchObjects(criteria, accessibleObject);
  }

  @Test
  public void testSearchObjectsFilterByObjectValueFromSameFact() {
    ObjectDocument accessibleObject = createObjectDocument().setValue("objectValueA");
    ObjectDocument inaccessibleObject = createObjectDocument().setValue("objectValueB");
    indexFact(d -> d.setObjects(set(accessibleObject, inaccessibleObject)));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addObjectValue(accessibleObject.getValue()));
    testSearchObjects(criteria, accessibleObject);
  }

  @Test
  public void testSearchObjectsFilterByKeywordsObjectValueFromDifferentFacts() {
    ObjectDocument accessibleObject = createObjectDocument().setValue("matching");
    indexFact(d -> d.setObjects(set(accessibleObject)));
    indexFact(d -> d.setObjects(set(createObjectDocument().setValue("something"))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("matching")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.objectValueText));
    testSearchObjects(criteria, accessibleObject);
  }

  @Test
  public void testSearchObjectsFilterByKeywordsObjectValueFromSameFact() {
    ObjectDocument accessibleObject = createObjectDocument().setValue("matching");
    ObjectDocument inaccessibleObject = createObjectDocument().setValue("something");
    indexFact(d -> d.setObjects(set(accessibleObject, inaccessibleObject)));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("matching")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.objectValueText));
    testSearchObjects(criteria, accessibleObject);
  }

  @Test
  public void testSearchObjectsFilterByMinimumFactsCount() {
    ObjectDocument accessibleObject = createObjectDocument();
    ObjectDocument inaccessibleObject = createObjectDocument();
    indexFact(d -> d.setObjects(set(accessibleObject)));
    indexFact(d -> d.setObjects(set(accessibleObject)));
    indexFact(d -> d.setObjects(set(inaccessibleObject)));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setMinimumFactsCount(2));
    testSearchObjectsWithoutCount(criteria, accessibleObject);
  }

  @Test
  public void testSearchObjectsFilterByMaximumFactsCount() {
    ObjectDocument accessibleObject = createObjectDocument();
    ObjectDocument inaccessibleObject = createObjectDocument();
    indexFact(d -> d.setObjects(set(accessibleObject)));
    indexFact(d -> d.setObjects(set(inaccessibleObject)));
    indexFact(d -> d.setObjects(set(inaccessibleObject)));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setMaximumFactsCount(1));
    testSearchObjectsWithoutCount(criteria, accessibleObject);
  }

  @Test
  public void testSearchObjectsFilterByMinMaxFactsCountWithAdditionalFactFilter() {
    UUID accessibleFactTypeID = UUID.randomUUID();
    UUID inaccessibleFactTypeID = UUID.randomUUID();
    ObjectDocument accessibleObject = createObjectDocument();
    ObjectDocument inaccessibleObject = createObjectDocument();
    indexFact(d -> d.setTypeID(accessibleFactTypeID).setObjects(set(accessibleObject)));
    indexFact(d -> d.setTypeID(accessibleFactTypeID).setObjects(set(accessibleObject)));
    indexFact(d -> d.setTypeID(inaccessibleFactTypeID).setObjects(set(accessibleObject)));
    indexFact(d -> d.setTypeID(accessibleFactTypeID).setObjects(set(inaccessibleObject)));
    indexFact(d -> d.setTypeID(inaccessibleFactTypeID).setObjects(set(inaccessibleObject)));
    indexFact(d -> d.setTypeID(inaccessibleFactTypeID).setObjects(set(inaccessibleObject)));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addFactTypeID(accessibleFactTypeID)
            .setMinimumFactsCount(2)
            .setMaximumFactsCount(2));
    testSearchObjectsWithoutCount(criteria, accessibleObject);
  }

  @Test
  public void testSearchObjectsReturnUniqueObjects() {
    ObjectDocument accessibleObject = createObjectDocument();
    indexFact(d -> d.setObjects(set(accessibleObject)));
    indexFact(d -> d.setObjects(set(accessibleObject)));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b);
    testSearchObjects(criteria, accessibleObject);
  }

  @Test
  public void testSearchObjectsPopulateSearchResult() {
    indexFact(d -> d);
    indexFact(d -> d);
    indexFact(d -> d);

    SearchResult<UUID> result = getFactSearchManager().searchObjects(createFactSearchCriteria(b -> b.setLimit(2)));
    assertEquals(2, result.getLimit());
    assertEquals(3, result.getCount());
    assertEquals(2, result.getValues().size());
  }

  @Test
  public void testSearchObjectsWithDailyIndices() {
    indexFact(d -> d.setLastSeenTimestamp(DAY1));
    indexFact(d -> d.setLastSeenTimestamp(DAY2));
    indexFact(d -> d.setLastSeenTimestamp(DAY3));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setIndexSelectCriteria(createIndexSelectCriteria(DAY2, DAY3)));
    testSearchObjects(criteria, 2);
  }

  @Test
  public void testSearchObjectsWithDailyIndicesIncludingTimeGlobal() {
    indexFact(d -> d.setLastSeenTimestamp(DAY1), FactSearchManager.TargetIndex.TimeGlobal);
    indexFact(d -> d.setLastSeenTimestamp(DAY2));
    indexFact(d -> d.setLastSeenTimestamp(DAY3));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setIndexSelectCriteria(createIndexSelectCriteria(DAY2, DAY3)));
    testSearchObjects(criteria, 3);
  }

  @Test
  public void testSearchObjectsWithDailyIndicesDeDuplicatesResult() {
    UUID factID = UUID.randomUUID();
    ObjectDocument object = createObjectDocument();
    indexFact(d -> d.setId(factID).setObjects(set(object)).setLastSeenTimestamp(DAY1));
    indexFact(d -> d.setId(factID).setObjects(set(object)).setLastSeenTimestamp(DAY2));
    indexFact(d -> d.setId(factID).setObjects(set(object)).setLastSeenTimestamp(DAY3));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b);
    testSearchObjects(criteria, 1);
  }

  private void testSearchObjects(FactSearchCriteria criteria, ObjectDocument accessibleObject) {
    SearchResult<UUID> result = getFactSearchManager().searchObjects(criteria);
    assertEquals(1, result.getCount());
    assertEquals(1, result.getValues().size());
    assertEquals(accessibleObject.getId(), result.getValues().get(0));
  }

  private void testSearchObjectsWithoutCount(FactSearchCriteria criteria, ObjectDocument accessibleObject) {
    SearchResult<UUID> result = getFactSearchManager().searchObjects(criteria);
    assertEquals(1, result.getValues().size());
    assertEquals(accessibleObject.getId(), result.getValues().get(0));
  }

  private void testSearchObjects(FactSearchCriteria criteria, long numberOfMatches) {
    SearchResult<UUID> result = getFactSearchManager().searchObjects(criteria);
    assertEquals(numberOfMatches, result.getValues().size());
  }

}
