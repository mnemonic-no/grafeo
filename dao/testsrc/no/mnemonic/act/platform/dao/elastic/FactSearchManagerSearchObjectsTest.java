package no.mnemonic.act.platform.dao.elastic;

import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.dao.elastic.document.SearchResult;
import org.junit.Test;

import java.util.UUID;

import static no.mnemonic.act.platform.dao.elastic.DocumentTestUtils.assertObjectDocument;
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
  public void testSearchObjectsAccessToOnlyPublicFact() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Public));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    FactSearchCriteria criteria = FactSearchCriteria.builder()
            .setCurrentUserID(UUID.randomUUID())
            .addAvailableOrganizationID(UUID.randomUUID())
            .build();

    testSearchObjects(criteria, first(accessibleFact.getObjects()));
  }

  @Test
  public void testSearchObjectsAccessToRoleBasedFactViaOrganization() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    FactSearchCriteria criteria = FactSearchCriteria.builder()
            .setCurrentUserID(UUID.randomUUID())
            .addAvailableOrganizationID(accessibleFact.getOrganizationID())
            .build();

    testSearchObjects(criteria, first(accessibleFact.getObjects()));
  }

  @Test
  public void testSearchObjectsAccessToRoleBasedFactViaACL() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    FactSearchCriteria criteria = FactSearchCriteria.builder()
            .setCurrentUserID(first(accessibleFact.getAcl()))
            .addAvailableOrganizationID(UUID.randomUUID())
            .build();

    testSearchObjects(criteria, first(accessibleFact.getObjects()));
  }

  @Test
  public void testSearchObjectsAccessToExplicitFact() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));

    FactSearchCriteria criteria = FactSearchCriteria.builder()
            .setCurrentUserID(first(accessibleFact.getAcl()))
            .addAvailableOrganizationID(UUID.randomUUID())
            .build();

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
  public void testSearchObjectsFilterByObjectTypeNameFromDifferentFacts() {
    ObjectDocument accessibleObject = createObjectDocument().setTypeName("objectTypeA");
    indexFact(d -> d.setObjects(set(accessibleObject)));
    indexFact(d -> d.setObjects(set(createObjectDocument().setTypeName("objectTypeB"))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addObjectTypeName(accessibleObject.getTypeName()));
    testSearchObjects(criteria, accessibleObject);
  }

  @Test
  public void testSearchObjectsFilterByObjectTypeNameFromSameFact() {
    ObjectDocument accessibleObject = createObjectDocument().setTypeName("objectTypeA");
    ObjectDocument inaccessibleObject = createObjectDocument().setTypeName("objectTypeB");
    indexFact(d -> d.setObjects(set(accessibleObject, inaccessibleObject)));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addObjectTypeName(accessibleObject.getTypeName()));
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
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.objectValue));
    testSearchObjects(criteria, accessibleObject);
  }

  @Test
  public void testSearchObjectsFilterByKeywordsObjectValueFromSameFact() {
    ObjectDocument accessibleObject = createObjectDocument().setValue("matching");
    ObjectDocument inaccessibleObject = createObjectDocument().setValue("something");
    indexFact(d -> d.setObjects(set(accessibleObject, inaccessibleObject)));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("matching")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.objectValue));
    testSearchObjects(criteria, accessibleObject);
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

    SearchResult<ObjectDocument> result = getFactSearchManager().searchObjects(createFactSearchCriteria(b -> b.setLimit(2)));
    assertEquals(2, result.getLimit());
    assertEquals(3, result.getCount());
    assertEquals(2, result.getValues().size());
  }

  private void testSearchObjects(FactSearchCriteria criteria, ObjectDocument accessibleObject) {
    SearchResult<ObjectDocument> result = getFactSearchManager().searchObjects(criteria);
    assertEquals(1, result.getCount());
    assertEquals(1, result.getValues().size());
    assertObjectDocument(accessibleObject, result.getValues().get(0));
  }

}
