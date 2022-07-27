package no.mnemonic.act.platform.dao.elastic;

import no.mnemonic.act.platform.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria.FactBinding;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.dao.elastic.result.ScrollingSearchResult;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static no.mnemonic.act.platform.dao.elastic.DocumentTestUtils.createObjectDocument;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FactSearchManagerSearchFactsTest extends AbstractManagerTest {

  @Test
  public void testSearchFactsWithNoCriteria() {
    assertNotNull(getFactSearchManager().searchFacts(null));
  }

  @Test
  public void testSearchFactsWithoutIndices() {
    testSearchFacts(createFactSearchCriteria(b -> b), 0);
  }

  @Test
  public void testSearchFactsAccessToOnlyPublicFact() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Public));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b);
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsAccessToRoleBasedFactViaOrganization() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b
            .setAccessControlCriteria(AccessControlCriteria.builder()
                    .addCurrentUserIdentity(UUID.randomUUID())
                    .addAvailableOrganizationID(accessibleFact.getOrganizationID())
                    .build()));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsAccessToRoleBasedFactViaACL() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b
            .setAccessControlCriteria(AccessControlCriteria.builder()
                    .addCurrentUserIdentity(first(accessibleFact.getAcl()))
                    .addAvailableOrganizationID(UUID.randomUUID())
                    .build()));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsAccessToExplicitFact() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));
    indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b
            .setAccessControlCriteria(AccessControlCriteria.builder()
                    .addCurrentUserIdentity(first(accessibleFact.getAcl()))
                    .addAvailableOrganizationID(UUID.randomUUID())
                    .build()));
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
  public void testSearchFactsFilterByOriginID() {
    FactDocument accessibleFact = indexFact(d -> d.setOriginID(UUID.randomUUID()));
    indexFact(d -> d.setOriginID(UUID.randomUUID()));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addOriginID(accessibleFact.getOriginID()));
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
  public void testSearchFactsFilterByObjectValue() {
    ObjectDocument accessibleObject = createObjectDocument().setValue("objectValueA");
    FactDocument accessibleFact = indexFact(d -> d.setObjects(set(accessibleObject)));
    indexFact(d -> d.setObjects(set(createObjectDocument().setValue("objectValueB"))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.addObjectValue(accessibleObject.getValue()));
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
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValueText));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchIp() {
    FactDocument accessibleFact = indexFact(d -> d.setValue("1.1.1.1"));
    indexFact(d -> d.setValue("2.2.2.2"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("1.1.1.0/24")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValueIp));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomain() {
    FactDocument accessibleFact = indexFact(d -> d.setValue("www.example.org"));
    indexFact(d -> d.setValue("www.test.com"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("example.org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValueDomain));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomainSameTld() {
    FactDocument accessibleFact = indexFact(d -> d.setValue("www.example.org"));
    indexFact(d -> d.setValue("www.test.org"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("example.org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValueDomain));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomainDifferentTld() {
    FactDocument accessibleFact = indexFact(d -> d.setValue("www.example.org"));
    indexFact(d -> d.setValue("www.example.com"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("example.org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValueDomain));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomainOnlyTld() {
    indexFact(d -> d.setValue("www.example.org"));
    indexFact(d -> d.setValue("www.example.com"));
    indexFact(d -> d.setValue("www.test.org"));
    indexFact(d -> d.setValue("www.test.com"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValueDomain));
    testSearchFacts(criteria, 2);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomainDifferentSubDomainPartial() {
    indexFact(d -> d.setValue("www.example.org"));
    indexFact(d -> d.setValue("mail.example.org"));
    indexFact(d -> d.setValue("nx.example.org"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("example.org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValueDomain));
    testSearchFacts(criteria, 3);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomainDifferentSubDomainFull() {
    FactDocument accessibleFact = indexFact(d -> d.setValue("www.example.org"));
    indexFact(d -> d.setValue("mail.example.org"));
    indexFact(d -> d.setValue("nx.example.org"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("www.example.org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValueDomain));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomainMultipleSubDomainsPartial() {
    indexFact(d -> d.setValue("a.www.example.org"));
    indexFact(d -> d.setValue("b.www.example.org"));
    indexFact(d -> d.setValue("c.www.example.org"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("www.example.org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValueDomain));
    testSearchFacts(criteria, 3);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomainMultipleSubDomainsFull() {
    FactDocument accessibleFact = indexFact(d -> d.setValue("a.www.example.org"));
    indexFact(d -> d.setValue("b.www.example.org"));
    indexFact(d -> d.setValue("c.www.example.org"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("a.www.example.org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValueDomain));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsFactValueMatchDomainIgnoreCaseSensitivity() {
    indexFact(d -> d.setValue("wWw.eXaMpLe.oRg"));
    indexFact(d -> d.setValue("www.EXAMPLE.org"));
    indexFact(d -> d.setValue("WWW.example.ORG"));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("WWW.EXAMPLE.ORG")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValueDomain));
    testSearchFacts(criteria, 3);
  }

  @Test
  public void testSearchFactsFilterByKeywordsObjectValueMatchText() {
    FactDocument accessibleFact = indexFact(d -> d.setObjects(set(createObjectDocument().setValue("matching"))));
    indexFact(d -> d.setObjects(set(createObjectDocument().setValue("something"))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("matching")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.objectValueText));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsObjectValueMatchIp() {
    FactDocument accessibleFact = indexFact(d -> d.setObjects(set(createObjectDocument().setValue("1.1.1.1"))));
    indexFact(d -> d.setObjects(set(createObjectDocument().setValue("2.2.2.2"))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("1.1.1.0/24")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.objectValueIp));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsObjectValueMatchDomain() {
    FactDocument accessibleFact = indexFact(d -> d.setObjects(set(createObjectDocument().setValue("www.example.org"))));
    indexFact(d -> d.setObjects(set(createObjectDocument().setValue("www.test.com"))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("example.org")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.objectValueDomain));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByKeywordsAllFields() {
    indexFact(d -> d.setValue("matching"));
    indexFact(d -> d.setObjects(set(createObjectDocument().setValue("matching"))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("matching")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.all));
    testSearchFacts(criteria, 2);
  }

  @Test
  public void testSearchFactsFilterByKeywordsMatchStrategyAll() {
    FactDocument accessibleFact = indexFact(d -> d.setValue("matching").setObjects(set(createObjectDocument().setValue("matching"))));
    indexFact(d -> d.setValue("matching").setObjects(set(createObjectDocument().setValue("something"))));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setKeywords("matching")
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.factValueText)
            .addKeywordFieldStrategy(FactSearchCriteria.KeywordFieldStrategy.objectValueText)
            .setKeywordMatchStrategy(FactSearchCriteria.MatchStrategy.all));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByStartTimestampDefaultSettings() {
    FactDocument accessibleFact = indexFact(d -> d.setTimestamp(DAY2).setLastSeenTimestamp(DAY2));
    indexFact(d -> d.setTimestamp(DAY2 - 2000).setLastSeenTimestamp(DAY2 - 2000));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setStartTimestamp(DAY2 - 1000));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByEndTimestampDefaultSettings() {
    FactDocument accessibleFact = indexFact(d -> d.setTimestamp(DAY2).setLastSeenTimestamp(DAY2));
    indexFact(d -> d.setTimestamp(DAY2 + 2000).setLastSeenTimestamp(DAY2 + 2000));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setEndTimestamp(DAY2 + 1000));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByTimestamp() {
    FactDocument accessibleFact = indexFact(d -> d.setTimestamp(DAY1).setLastSeenTimestamp(DAY3));
    indexFact(d -> d.setTimestamp(DAY1 - 2000).setLastSeenTimestamp(DAY3));
    indexFact(d -> d.setTimestamp(DAY1 + 2000).setLastSeenTimestamp(DAY3));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setStartTimestamp(DAY1 - 1000)
            .setEndTimestamp(DAY1 + 1000)
            .addTimeFieldStrategy(FactSearchCriteria.TimeFieldStrategy.timestamp));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByLastSeenTimestamp() {
    FactDocument accessibleFact = indexFact(d -> d.setTimestamp(DAY1).setLastSeenTimestamp(DAY3));
    indexFact(d -> d.setTimestamp(DAY1).setLastSeenTimestamp(DAY3 - 2000));
    indexFact(d -> d.setTimestamp(DAY1).setLastSeenTimestamp(DAY3 + 2000));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setStartTimestamp(DAY3 - 1000)
            .setEndTimestamp(DAY3 + 1000)
            .addTimeFieldStrategy(FactSearchCriteria.TimeFieldStrategy.lastSeenTimestamp));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByAllTimestamps() {
    indexFact(d -> d.setTimestamp(DAY1).setLastSeenTimestamp(DAY2));
    indexFact(d -> d.setTimestamp(DAY2).setLastSeenTimestamp(DAY3));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setStartTimestamp(DAY2 - 1000)
            .setEndTimestamp(DAY2 + 1000)
            .addTimeFieldStrategy(FactSearchCriteria.TimeFieldStrategy.all));
    testSearchFacts(criteria, 2);
  }

  @Test
  public void testSearchFactsFilterByTimestampsMatchStrategyAll() {
    FactDocument accessibleFact = indexFact(d -> d.setTimestamp(DAY2).setLastSeenTimestamp(DAY2));
    indexFact(d -> d.setTimestamp(DAY1).setLastSeenTimestamp(DAY2));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setStartTimestamp(DAY2 - 1000)
            .setEndTimestamp(DAY2 + 1000)
            .addTimeFieldStrategy(FactSearchCriteria.TimeFieldStrategy.all)
            .setTimeMatchStrategy(FactSearchCriteria.MatchStrategy.all));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByNumberTrust() {
    FactDocument accessibleFact = indexFact(d -> d.setTrust(0.2f));
    indexFact(d -> d.setTrust(0.4f));
    indexFact(d -> d.setTrust(0.6f));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setMinNumber(0.1f)
            .setMaxNumber(0.3f)
            .addNumberFieldStrategy(FactSearchCriteria.NumberFieldStrategy.trust));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByNumberConfidence() {
    FactDocument accessibleFact = indexFact(d -> d.setConfidence(0.2f));
    indexFact(d -> d.setConfidence(0.4f));
    indexFact(d -> d.setConfidence(0.6f));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setMinNumber(0.1f)
            .setMaxNumber(0.3f)
            .addNumberFieldStrategy(FactSearchCriteria.NumberFieldStrategy.confidence));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByNumberCertainty() {
    FactDocument accessibleFact = indexFact(d -> d.setTrust(1.0f).setConfidence(0.2f));
    indexFact(d -> d.setTrust(1.0f).setConfidence(0.4f));
    indexFact(d -> d.setTrust(1.0f).setConfidence(0.6f));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setMinNumber(0.1f)
            .setMaxNumber(0.3f)
            .addNumberFieldStrategy(FactSearchCriteria.NumberFieldStrategy.certainty));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsFilterByFactBinding() {
    ObjectDocument obj1 = createObjectDocument();
    ObjectDocument obj2 = createObjectDocument();

    FactDocument oneLeggedFact = indexFact(d -> d.setObjects(set(obj1)));
    FactDocument twoLeggedFact = indexFact(d -> d.setObjects(set(obj1, obj2)));
    FactDocument metaFact = indexFact(d -> d.setObjects(set()));

    testSearchFacts(createFactSearchCriteria(c -> c.setFactBinding(FactBinding.meta)), metaFact);
    testSearchFacts(createFactSearchCriteria(c -> c.setFactBinding(FactBinding.oneLegged)), oneLeggedFact);
    testSearchFacts(createFactSearchCriteria(c -> c.setFactBinding(FactBinding.twoLegged)), twoLeggedFact);
  }

  @Test
  public void testSearchFactsFilterByNumberFieldStrategyAll() {
    indexFact(d -> d.setTrust(0.2f).setConfidence(0.4f));
    indexFact(d -> d.setTrust(0.4f).setConfidence(0.2f));
    indexFact(d -> d.setTrust(0.5f).setConfidence(0.5f));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setMinNumber(0.1f)
            .setMaxNumber(0.3f)
            .addNumberFieldStrategy(FactSearchCriteria.NumberFieldStrategy.all));
    testSearchFacts(criteria, 3);
  }

  @Test
  public void testSearchFactsFilterByNumberMatchStrategyAll() {
    FactDocument accessibleFact = indexFact(d -> d.setTrust(0.4f).setConfidence(0.5f));
    indexFact(d -> d.setTrust(0.5f).setConfidence(0.9f));
    indexFact(d -> d.setTrust(0.8f).setConfidence(0.4f));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setMinNumber(0.1f)
            .setMaxNumber(0.6f)
            .setNumberMatchStrategy(FactSearchCriteria.MatchStrategy.all));
    testSearchFacts(criteria, accessibleFact);
  }

  @Test
  public void testSearchFactsPopulateSearchResult() {
    indexFact(d -> d);
    indexFact(d -> d);
    indexFact(d -> d);

    ScrollingSearchResult<UUID> result = getFactSearchManager().searchFacts(createFactSearchCriteria(b -> b));
    assertEquals(3, result.getCount());
    assertEquals(3, ListUtils.list(result).size());
  }

  @Test
  public void testSearchFactsWithDailyIndices() {
    indexFact(d -> d.setLastSeenTimestamp(DAY1));
    indexFact(d -> d.setLastSeenTimestamp(DAY2));
    indexFact(d -> d.setLastSeenTimestamp(DAY3));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setIndexSelectCriteria(createIndexSelectCriteria(DAY2, DAY3)));
    testSearchFacts(criteria, 2);
  }

  @Test
  public void testSearchFactsWithDailyIndicesIncludingTimeGlobal() {
    indexFact(d -> d.setLastSeenTimestamp(DAY1), FactSearchManager.TargetIndex.TimeGlobal);
    indexFact(d -> d.setLastSeenTimestamp(DAY2));
    indexFact(d -> d.setLastSeenTimestamp(DAY3));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b.setIndexSelectCriteria(createIndexSelectCriteria(DAY2, DAY3)));
    testSearchFacts(criteria, 3);
  }

  @Test
  public void testSearchFactsWithDailyIndicesDeDuplicatesResult() {
    UUID id = UUID.randomUUID();
    indexFact(d -> d.setId(id).setLastSeenTimestamp(DAY1));
    indexFact(d -> d.setId(id).setLastSeenTimestamp(DAY2));
    indexFact(d -> d.setId(id).setLastSeenTimestamp(DAY3));

    FactSearchCriteria criteria = createFactSearchCriteria(b -> b);
    testSearchFacts(criteria, 1);
  }

  private void testSearchFacts(FactSearchCriteria criteria, FactDocument accessibleFact) {
    List<UUID> result = ListUtils.list(getFactSearchManager().searchFacts(criteria));
    assertEquals(1, result.size());
    assertEquals(accessibleFact.getId(), result.get(0));
  }

  private void testSearchFacts(FactSearchCriteria criteria, int numberOfMatches) {
    List<UUID> result = ListUtils.list(getFactSearchManager().searchFacts(criteria));
    assertEquals(numberOfMatches, result.size());
  }

}
