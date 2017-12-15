package no.mnemonic.act.platform.dao.elastic;

import no.mnemonic.act.platform.dao.api.FactSearchCriteria;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static no.mnemonic.act.platform.dao.elastic.document.DocumentTestUtils.*;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FactSearchManagerTest extends AbstractManagerTest {

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
  public void testIndexFactEncodesValues() {
    getFactSearchManager().indexFact(createFactDocument());
    verify(getEntityHandler(), times(2)).encode(any());
  }

  @Test
  public void testGetFactDecodesValues() {
    FactDocument fact = createFactDocument();
    getFactSearchManager().indexFact(fact);
    getFactSearchManager().getFact(fact.getId());
    verify(getEntityHandler(), times(2)).decode(any());
  }

  @Test
  public void testSearchFactsWithNoCriteria() {
    assertNotNull(getFactSearchManager().searchFacts(null));
  }

  @Test
  public void testSearchFactsAccessToOnlyPublicFact() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Public));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    FactSearchCriteria criteria = FactSearchCriteria.builder()
            .setCurrentUserID(UUID.randomUUID())
            .addAvailableOrganizationID(UUID.randomUUID())
            .build();

    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsAccessToRoleBasedFactViaOrganization() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    FactSearchCriteria criteria = FactSearchCriteria.builder()
            .setCurrentUserID(UUID.randomUUID())
            .addAvailableOrganizationID(accessibleFact.getOrganizationID())
            .build();

    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsAccessToRoleBasedFactViaACL() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    FactSearchCriteria criteria = FactSearchCriteria.builder()
            .setCurrentUserID(accessibleFact.getAcl().iterator().next())
            .addAvailableOrganizationID(UUID.randomUUID())
            .build();

    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsAccessToExplicitFact() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));

    FactSearchCriteria criteria = FactSearchCriteria.builder()
            .setCurrentUserID(accessibleFact.getAcl().iterator().next())
            .addAvailableOrganizationID(UUID.randomUUID())
            .build();

    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByFactID() {
    FactDocument accessibleFact = indexFact(d -> d.setId(UUID.randomUUID()));
    indexFact(d -> d.setId(UUID.randomUUID()));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addFactID(accessibleFact.getId()));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByFactTypeID() {
    FactDocument accessibleFact = indexFact(d -> d.setTypeID(UUID.randomUUID()));
    indexFact(d -> d.setTypeID(UUID.randomUUID()));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addFactTypeID(accessibleFact.getTypeID()));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByFactTypeName() {
    FactDocument accessibleFact = indexFact(d -> d.setTypeName("factTypeA"));
    indexFact(d -> d.setTypeName("factTypeB"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addFactTypeName(accessibleFact.getTypeName()));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByFactValue() {
    FactDocument accessibleFact = indexFact(d -> d.setValue("factValueA"));
    indexFact(d -> d.setValue("factValueB"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addFactValue(accessibleFact.getValue()));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByInReferenceTo() {
    FactDocument accessibleFact = indexFact(d -> d.setInReferenceTo(UUID.randomUUID()));
    indexFact(d -> d.setInReferenceTo(UUID.randomUUID()));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addInReferenceTo(accessibleFact.getInReferenceTo()));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByOrganizationID() {
    FactDocument accessibleFact = indexFact(d -> d.setOrganizationID(UUID.randomUUID()));
    indexFact(d -> d.setOrganizationID(UUID.randomUUID()));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addOrganizationID(accessibleFact.getOrganizationID()));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByOrganizationName() {
    FactDocument accessibleFact = indexFact(d -> d.setOrganizationName("organizationA"));
    indexFact(d -> d.setOrganizationName("organizationB"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addOrganizationName(accessibleFact.getOrganizationName()));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterBySourceID() {
    FactDocument accessibleFact = indexFact(d -> d.setSourceID(UUID.randomUUID()));
    indexFact(d -> d.setSourceID(UUID.randomUUID()));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addSourceID(accessibleFact.getSourceID()));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterBySourceName() {
    FactDocument accessibleFact = indexFact(d -> d.setSourceName("sourceA"));
    indexFact(d -> d.setSourceName("sourceB"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addSourceName(accessibleFact.getSourceName()));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByObjectID() {
    ObjectDocument accessibleObject = createObjectDocument().setId(UUID.randomUUID());
    FactDocument accessibleFact = indexFact(d -> d.setObjects(set(accessibleObject)));
    indexFact(d -> d.setObjects(set(createObjectDocument().setId(UUID.randomUUID()))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addObjectID(accessibleObject.getId()));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByObjectTypeID() {
    ObjectDocument accessibleObject = createObjectDocument().setTypeID(UUID.randomUUID());
    FactDocument accessibleFact = indexFact(d -> d.setObjects(set(accessibleObject)));
    indexFact(d -> d.setObjects(set(createObjectDocument().setTypeID(UUID.randomUUID()))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addObjectTypeID(accessibleObject.getTypeID()));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByObjectTypeName() {
    ObjectDocument accessibleObject = createObjectDocument().setTypeName("objectTypeA");
    FactDocument accessibleFact = indexFact(d -> d.setObjects(set(accessibleObject)));
    indexFact(d -> d.setObjects(set(createObjectDocument().setTypeName("objectTypeB"))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addObjectTypeName(accessibleObject.getTypeName()));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByObjectValue() {
    ObjectDocument accessibleObject = createObjectDocument().setValue("objectValueA");
    FactDocument accessibleFact = indexFact(d -> d.setObjects(set(accessibleObject)));
    indexFact(d -> d.setObjects(set(createObjectDocument().setValue("objectValueB"))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addObjectValue(accessibleObject.getValue()));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByRetractedTrue() {
    FactDocument accessibleFact = indexFact(d -> d.setRetracted(true));
    indexFact(d -> d.setRetracted(false));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setRetracted(true));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByRetractedFalse() {
    FactDocument accessibleFact = indexFact(d -> d.setRetracted(false));
    indexFact(d -> d.setRetracted(true));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setRetracted(false));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsDefaultSettings() {
    FactDocument accessibleFact = indexFact(d -> d.setValue("matching value"));
    indexFact(d -> d.setValue("not matching value"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("+-not +matching"));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchText() {
    FactDocument accessibleFact = indexFact(d -> d.setValue("matching"));
    indexFact(d -> d.setValue("something"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("matching")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValue));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchIp() {
    FactDocument accessibleFact = indexFact(d -> d.setValue("1.1.1.1"));
    indexFact(d -> d.setValue("2.2.2.2"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("1.1.1.0/24")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValue));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomain() {
    FactDocument accessibleFact = indexFact(d -> d.setValue("www.example.org"));
    indexFact(d -> d.setValue("www.test.com"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("example.org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValue));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomainSameTld() {
    FactDocument accessibleFact = indexFact(d -> d.setValue("www.example.org"));
    indexFact(d -> d.setValue("www.test.org"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("example.org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValue));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomainDifferentTld() {
    FactDocument accessibleFact = indexFact(d -> d.setValue("www.example.org"));
    indexFact(d -> d.setValue("www.example.com"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("example.org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValue));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomainOnlyTld() {
    indexFact(d -> d.setValue("www.example.org"));
    indexFact(d -> d.setValue("www.example.com"));
    indexFact(d -> d.setValue("www.test.org"));
    indexFact(d -> d.setValue("www.test.com"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValue));
    testSearchFacts(criteria, 2);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomainDifferentSubDomainPartial() {
    indexFact(d -> d.setValue("www.example.org"));
    indexFact(d -> d.setValue("mail.example.org"));
    indexFact(d -> d.setValue("nx.example.org"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("example.org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValue));
    testSearchFacts(criteria, 3);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomainDifferentSubDomainFull() {
    FactDocument accessibleFact = indexFact(d -> d.setValue("www.example.org"));
    indexFact(d -> d.setValue("mail.example.org"));
    indexFact(d -> d.setValue("nx.example.org"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("www.example.org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValue));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomainMultipleSubDomainsPartial() {
    indexFact(d -> d.setValue("a.www.example.org"));
    indexFact(d -> d.setValue("b.www.example.org"));
    indexFact(d -> d.setValue("c.www.example.org"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("www.example.org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValue));
    testSearchFacts(criteria, 3);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomainMultipleSubDomainsFull() {
    FactDocument accessibleFact = indexFact(d -> d.setValue("a.www.example.org"));
    indexFact(d -> d.setValue("b.www.example.org"));
    indexFact(d -> d.setValue("c.www.example.org"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("a.www.example.org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValue));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomainIgnoreCaseSensitivity() {
    indexFact(d -> d.setValue("wWw.eXaMpLe.oRg"));
    indexFact(d -> d.setValue("www.EXAMPLE.org"));
    indexFact(d -> d.setValue("WWW.example.ORG"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("WWW.EXAMPLE.ORG")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValue));
    testSearchFacts(criteria, 3);
  }

  @Test
  public void testSearchFactsFilterByKeywordsObjectValueMatchText() {
    FactDocument accessibleFact = indexFact(d -> d.setObjects(set(createObjectDocument().setValue("matching"))));
    indexFact(d -> d.setObjects(set(createObjectDocument().setValue("something"))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("matching")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.objectValue));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsObjectValueMatchIp() {
    FactDocument accessibleFact = indexFact(d -> d.setObjects(set(createObjectDocument().setValue("1.1.1.1"))));
    indexFact(d -> d.setObjects(set(createObjectDocument().setValue("2.2.2.2"))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("1.1.1.0/24")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.objectValue));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsObjectValueMatchDomain() {
    FactDocument accessibleFact = indexFact(d -> d.setObjects(set(createObjectDocument().setValue("www.example.org"))));
    indexFact(d -> d.setObjects(set(createObjectDocument().setValue("www.test.com"))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("example.org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.objectValue));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsOrganization() {
    FactDocument accessibleFact = indexFact(d -> d.setOrganizationName("organization"));
    indexFact(d -> d.setOrganizationName("something"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("org*")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.organization));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsSource() {
    FactDocument accessibleFact = indexFact(d -> d.setSourceName("organization"));
    indexFact(d -> d.setSourceName("something"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("org*")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.source));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsAllFields() {
    indexFact(d -> d.setValue("matching"));
    indexFact(d -> d.setOrganizationName("matching"));
    indexFact(d -> d.setSourceName("matching"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("matching")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.all));
    testSearchFacts(criteria, 3);
  }

  @Test
  public void testSearchFactsFilterByKeywordsMatchStrategyAll() {
    FactDocument accessibleFact = indexFact(d -> d.setOrganizationName("matching").setSourceName("matching"));
    indexFact(d -> d.setOrganizationName("something").setSourceName("matching"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("matching")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.organization)
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.source)
            .setKeywordMatchStrategy(FactSearchCriteria.MatchStrategy.all));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByStartTimestampDefaultSettings() {
    long now = System.currentTimeMillis();
    FactDocument accessibleFact = indexFact(d -> d.setTimestamp(now).setLastSeenTimestamp(now));
    indexFact(d -> d.setTimestamp(now - 2000).setLastSeenTimestamp(now - 2000));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setStartTimestamp(now - 1000));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByEndTimestampDefaultSettings() {
    long now = System.currentTimeMillis();
    FactDocument accessibleFact = indexFact(d -> d.setTimestamp(now).setLastSeenTimestamp(now));
    indexFact(d -> d.setTimestamp(now + 2000).setLastSeenTimestamp(now + 2000));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setEndTimestamp(now + 1000));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByTimestamp() {
    long now = System.currentTimeMillis();
    FactDocument accessibleFact = indexFact(d -> d.setTimestamp(now).setLastSeenTimestamp(0));
    indexFact(d -> d.setTimestamp(now - 2000).setLastSeenTimestamp(0));
    indexFact(d -> d.setTimestamp(now + 2000).setLastSeenTimestamp(0));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setStartTimestamp(now - 1000)
            .setEndTimestamp(now + 1000)
            .addTimeFieldStrategy(FactSearchCriteria.TimeFieldStrategy.timestamp));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByLastSeenTimestamp() {
    long now = System.currentTimeMillis();
    FactDocument accessibleFact = indexFact(d -> d.setTimestamp(0).setLastSeenTimestamp(now));
    indexFact(d -> d.setTimestamp(0).setLastSeenTimestamp(now - 2000));
    indexFact(d -> d.setTimestamp(0).setLastSeenTimestamp(now + 2000));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setStartTimestamp(now - 1000)
            .setEndTimestamp(now + 1000)
            .addTimeFieldStrategy(FactSearchCriteria.TimeFieldStrategy.lastSeenTimestamp));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByAllTimestamps() {
    long now = System.currentTimeMillis();
    indexFact(d -> d.setTimestamp(0).setLastSeenTimestamp(now));
    indexFact(d -> d.setTimestamp(now).setLastSeenTimestamp(0));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setStartTimestamp(now - 1000)
            .setEndTimestamp(now + 1000)
            .addTimeFieldStrategy(FactSearchCriteria.TimeFieldStrategy.all));
    testSearchFacts(criteria, 2);
  }

  @Test
  public void testSearchFactsFilterByTimestampsMatchStrategyAll() {
    long now = System.currentTimeMillis();
    FactDocument accessibleFact = indexFact(d -> d.setTimestamp(now).setLastSeenTimestamp(now));
    indexFact(d -> d.setTimestamp(0).setLastSeenTimestamp(now));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setStartTimestamp(now - 1000)
            .setEndTimestamp(now + 1000)
            .addTimeFieldStrategy(FactSearchCriteria.TimeFieldStrategy.all)
            .setTimeMatchStrategy(FactSearchCriteria.MatchStrategy.all));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsLimitResults() {
    testSearchFactsWithLimit(2, 2);
  }

  @Test
  public void testSearchFactsLimitResultsNegativeLimit() {
    testSearchFactsWithLimit(-1, 3);
  }

  @Test
  public void testSearchFactsLimitResultsOverMaxResultWindowLimit() {
    testSearchFactsWithLimit(Integer.MAX_VALUE, 3);
  }

  private FactSearchCriteria createFactSearchCriteria(ObjectPreparation<FactSearchCriteria.Builder> preparation) {
    FactSearchCriteria.Builder builder = FactSearchCriteria.builder()
            .setCurrentUserID(UUID.randomUUID())
            .addAvailableOrganizationID(UUID.randomUUID());
    if (preparation != null) {
      builder = preparation.prepare(builder);
    }
    return builder.build();
  }

  private FactDocument indexFact(ObjectPreparation<FactDocument> preparation) {
    FactDocument document = preparation != null ? preparation.prepare(createFactDocument()) : createFactDocument();
    return getFactSearchManager().indexFact(document);
  }

  private void testSearchFacts(FactSearchCriteria criteria, FactDocument accessibleFact) {
    refreshIndices(); // Refresh indices in order to make Facts available in search.
    List<FactDocument> result = getFactSearchManager().searchFacts(criteria);
    assertEquals(1, result.size());
    assertFactDocument(accessibleFact, result.get(0));
  }

  private void testSearchFacts(FactSearchCriteria criteria, int numberOfMatches) {
    refreshIndices(); // Refresh indices in order to make Facts available in search.
    List<FactDocument> result = getFactSearchManager().searchFacts(criteria);
    assertEquals(numberOfMatches, result.size());
  }

  private void testSearchFactsWithLimit(int limit, int numberOfExpectedResults) {
    indexFact(d -> d);
    indexFact(d -> d);
    indexFact(d -> d);
    refreshIndices();

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setLimit(limit));
    List<FactDocument> result = getFactSearchManager().searchFacts(criteria);
    assertEquals(numberOfExpectedResults, result.size());
  }

  interface ObjectPreparation<T> {
    T prepare(T e);
  }

}
