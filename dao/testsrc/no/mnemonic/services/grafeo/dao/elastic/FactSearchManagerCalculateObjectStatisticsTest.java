package no.mnemonic.services.grafeo.dao.elastic;

import no.mnemonic.services.grafeo.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.ObjectStatisticsCriteria;
import no.mnemonic.services.grafeo.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.services.grafeo.dao.elastic.document.FactDocument;
import no.mnemonic.services.grafeo.dao.elastic.document.ObjectDocument;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static no.mnemonic.services.grafeo.dao.elastic.DocumentTestUtils.createObjectDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FactSearchManagerCalculateObjectStatisticsTest extends AbstractManagerTest {

  @Test
  public void testCalculateObjectStatisticsWithNoCriteria() {
    assertNotNull(getFactSearchManager().calculateObjectStatistics(null));
  }

  @Test
  public void testCalculateObjectStatisticsWithoutIndices() {
    assertEquals(0, executeCalculateObjectStatistics(UUID.randomUUID()).getStatisticsCount());
  }

  @Test
  public void testCalculateObjectStatisticsAccessToOnlyPublicFact() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Public));
    FactDocument nonAccessibleFact1 = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    FactDocument nonAccessibleFact2 = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    ObjectStatisticsCriteria criteria = createObjectStatisticsCriteria(b -> b
            .addObjectID(getFirstObjectID(accessibleFact))
            .addObjectID(getFirstObjectID(nonAccessibleFact1))
            .addObjectID(getFirstObjectID(nonAccessibleFact2)));
    assertSingleStatisticExists(criteria, getFirstObjectID(accessibleFact));
  }

  @Test
  public void testCalculateObjectStatisticsAccessToRoleBasedFactViaOrganization() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    FactDocument nonAccessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    ObjectStatisticsCriteria criteria = createObjectStatisticsCriteria(b -> b
            .setAccessControlCriteria(AccessControlCriteria.builder()
                    .addCurrentUserIdentity(UUID.randomUUID())
                    .addAvailableOrganizationID(accessibleFact.getOrganizationID())
                    .build())
            .addObjectID(getFirstObjectID(accessibleFact))
            .addObjectID(getFirstObjectID(nonAccessibleFact)));
    assertSingleStatisticExists(criteria, getFirstObjectID(accessibleFact));
  }

  @Test
  public void testCalculateObjectStatisticsAccessToRoleBasedFactViaACL() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));
    FactDocument nonAccessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));

    ObjectStatisticsCriteria criteria = createObjectStatisticsCriteria(b -> b
            .setAccessControlCriteria(AccessControlCriteria.builder()
                    .addCurrentUserIdentity(first(accessibleFact.getAcl()))
                    .addAvailableOrganizationID(UUID.randomUUID())
                    .build())
            .addObjectID(getFirstObjectID(accessibleFact))
            .addObjectID(getFirstObjectID(nonAccessibleFact)));
    assertSingleStatisticExists(criteria, getFirstObjectID(accessibleFact));
  }

  @Test
  public void testCalculateObjectStatisticsAccessToExplicitFact() {
    FactDocument accessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.Explicit));
    FactDocument nonAccessibleFact = indexFact(d -> d.setAccessMode(FactDocument.AccessMode.RoleBased));

    ObjectStatisticsCriteria criteria = createObjectStatisticsCriteria(b -> b
            .setAccessControlCriteria(AccessControlCriteria.builder()
                    .addCurrentUserIdentity(first(accessibleFact.getAcl()))
                    .addAvailableOrganizationID(UUID.randomUUID())
                    .build())
            .addObjectID(getFirstObjectID(accessibleFact))
            .addObjectID(getFirstObjectID(nonAccessibleFact)));
    assertSingleStatisticExists(criteria, getFirstObjectID(accessibleFact));
  }

  @Test
  public void testCalculateObjectStatisticsFilterByObjectIdFromDifferentFacts() {
    ObjectDocument accessibleObject = createObjectDocument();
    ObjectDocument inaccessibleObject = createObjectDocument();
    indexFact(d -> d.setObjects(set(accessibleObject)));
    indexFact(d -> d.setObjects(set(inaccessibleObject)));

    ObjectStatisticsCriteria criteria = createObjectStatisticsCriteria(b -> b.addObjectID(accessibleObject.getId()));
    assertSingleStatisticExists(criteria, accessibleObject.getId());
  }

  @Test
  public void testCalculateObjectStatisticsFilterByObjectIdFromSameFact() {
    ObjectDocument accessibleObject = createObjectDocument();
    ObjectDocument inaccessibleObject = createObjectDocument();
    indexFact(d -> d.setObjects(set(accessibleObject, inaccessibleObject)));

    ObjectStatisticsCriteria criteria = createObjectStatisticsCriteria(b -> b.addObjectID(accessibleObject.getId()));
    assertSingleStatisticExists(criteria, accessibleObject.getId());
  }

  @Test
  public void testCalculateObjectStatisticsForMultipleObjectsFromDifferentFacts() {
    ObjectDocument object1 = createObjectDocument();
    ObjectDocument object2 = createObjectDocument();
    indexFact(d -> d.setObjects(set(object1)));
    indexFact(d -> d.setObjects(set(object2)));

    ObjectStatisticsContainer result = executeCalculateObjectStatistics(object1.getId(), object2.getId());
    assertEquals(2, result.getStatisticsCount());
  }

  @Test
  public void testCalculateObjectStatisticsForMultipleObjectsFromSameFact() {
    ObjectDocument object1 = createObjectDocument();
    ObjectDocument object2 = createObjectDocument();
    indexFact(d -> d.setObjects(set(object1, object2)));

    ObjectStatisticsContainer result = executeCalculateObjectStatistics(object1.getId(), object2.getId());
    assertEquals(2, result.getStatisticsCount());
  }

  @Test
  public void testCalculateObjectStatisticsForObjectWithSingleFact() {
    ObjectDocument object = createObjectDocument();
    FactDocument fact = indexFact(d -> d.setObjects(set(object)));

    ObjectStatisticsContainer result = executeCalculateObjectStatistics(object.getId());
    ObjectStatisticsContainer.FactStatistic statistic = getFirstStatistic(result, object.getId());
    assertEquals(1, statistic.getFactCount());
    assertEquals(fact.getTypeID(), statistic.getFactTypeID());
    assertEquals(fact.getTimestamp(), statistic.getLastAddedTimestamp());
    assertEquals(fact.getLastSeenTimestamp(), statistic.getLastSeenTimestamp());
  }

  @Test
  public void testCalculateObjectStatisticsForObjectWithMultipleFactsOfSameType() {
    UUID typeID = UUID.randomUUID();
    ObjectDocument object = createObjectDocument();
    indexFact(d -> d.setTypeID(typeID)
            .setTimestamp(DAY2)
            .setLastSeenTimestamp(DAY2)
            .setObjects(set(object))
    );
    indexFact(d -> d.setTypeID(typeID)
            .setTimestamp(DAY1)
            .setLastSeenTimestamp(DAY3)
            .setObjects(set(object))
    );

    ObjectStatisticsContainer result = executeCalculateObjectStatistics(object.getId());
    ObjectStatisticsContainer.FactStatistic statistic = getFirstStatistic(result, object.getId());
    assertEquals(typeID, statistic.getFactTypeID());
    assertEquals(2, statistic.getFactCount());
    assertEquals(DAY2, statistic.getLastAddedTimestamp());
    assertEquals(DAY3, statistic.getLastSeenTimestamp());
  }

  @Test
  public void testCalculateObjectStatisticsForObjectWithMultipleFactsOfDifferentTypes() {
    ObjectDocument object = createObjectDocument();
    indexFact(d -> d.setTypeID(UUID.randomUUID()).setObjects(set(object)));
    indexFact(d -> d.setTypeID(UUID.randomUUID()).setObjects(set(object)));

    ObjectStatisticsContainer result = executeCalculateObjectStatistics(object.getId());
    assertEquals(1, result.getStatisticsCount());
    assertEquals(2, result.getStatistics(object.getId()).size());
  }

  @Test
  public void testCalculateObjectStatisticsOmitsFactsLastSeenBeforeStartTimestamp() {
    UUID typeID = UUID.randomUUID();
    ObjectDocument object = createObjectDocument();
    indexFact(d -> d.setTypeID(typeID)
            .setTimestamp(DAY2)
            .setLastSeenTimestamp(DAY2)
            .addObject(object)
    );
    indexFact(d -> d.setTypeID(typeID)
            .setTimestamp(DAY1)
            .setLastSeenTimestamp(DAY3)
            .addObject(object)
    );

    ObjectStatisticsCriteria criteria = createObjectStatisticsCriteria(b -> b.addObjectID(object.getId()).setStartTimestamp(DAY3 - 1000));
    ObjectStatisticsContainer.FactStatistic statistic = getFirstStatistic(getFactSearchManager().calculateObjectStatistics(criteria), object.getId());
    assertEquals(typeID, statistic.getFactTypeID());
    assertEquals(1, statistic.getFactCount());
    assertEquals(DAY1, statistic.getLastAddedTimestamp());
    assertEquals(DAY3, statistic.getLastSeenTimestamp());
  }

  @Test
  public void testCalculateObjectStatisticsOmitsFactsLastSeenAfterEndTimestamp() {
    UUID typeID = UUID.randomUUID();
    ObjectDocument object = createObjectDocument();
    indexFact(d -> d.setTypeID(typeID)
            .setTimestamp(DAY2)
            .setLastSeenTimestamp(DAY2)
            .addObject(object)
    );
    indexFact(d -> d.setTypeID(typeID)
            .setTimestamp(DAY1)
            .setLastSeenTimestamp(DAY3)
            .addObject(object)
    );

    ObjectStatisticsCriteria criteria = createObjectStatisticsCriteria(b -> b.addObjectID(object.getId()).setEndTimestamp(DAY3 - 1000));
    ObjectStatisticsContainer.FactStatistic statistic = getFirstStatistic(getFactSearchManager().calculateObjectStatistics(criteria), object.getId());
    assertEquals(typeID, statistic.getFactTypeID());
    assertEquals(1, statistic.getFactCount());
    assertEquals(DAY2, statistic.getLastAddedTimestamp());
    assertEquals(DAY2, statistic.getLastSeenTimestamp());
  }

  @Test
  public void testCalculateObjectStatisticsWithDailyIndices() {
    ObjectDocument object = createObjectDocument();
    indexFact(d -> d.setObjects(set(object)).setLastSeenTimestamp(DAY1));
    indexFact(d -> d.setObjects(set(object)).setLastSeenTimestamp(DAY2));
    indexFact(d -> d.setObjects(set(object)).setLastSeenTimestamp(DAY3));

    ObjectStatisticsCriteria criteria = createObjectStatisticsCriteria(b -> b.addObjectID(object.getId())
            .setIndexSelectCriteria(createIndexSelectCriteria(DAY2, DAY3)));
    assertEquals(2, getFactSearchManager().calculateObjectStatistics(criteria).getStatistics(object.getId()).size());
  }

  @Test
  public void testCalculateObjectStatisticsWithDailyIndicesIncludingTimeGlobal() {
    ObjectDocument object = createObjectDocument();
    indexFact(d -> d.setObjects(set(object)).setLastSeenTimestamp(DAY1), FactSearchManager.TargetIndex.TimeGlobal);
    indexFact(d -> d.setObjects(set(object)).setLastSeenTimestamp(DAY2));
    indexFact(d -> d.setObjects(set(object)).setLastSeenTimestamp(DAY3));

    ObjectStatisticsCriteria criteria = createObjectStatisticsCriteria(b -> b.addObjectID(object.getId())
            .setIndexSelectCriteria(createIndexSelectCriteria(DAY2, DAY3)));
    assertEquals(3, getFactSearchManager().calculateObjectStatistics(criteria).getStatistics(object.getId()).size());
  }

  @Test
  public void testCalculateObjectStatisticsWithDailyIndicesDeDuplicatesResult() {
    UUID factID = UUID.randomUUID();
    UUID factTypeID = UUID.randomUUID();
    ObjectDocument object = createObjectDocument();
    indexFact(d -> d.setId(factID).setTypeID(factTypeID).setObjects(set(object)).setLastSeenTimestamp(DAY1));
    indexFact(d -> d.setId(factID).setTypeID(factTypeID).setObjects(set(object)).setLastSeenTimestamp(DAY2));
    indexFact(d -> d.setId(factID).setTypeID(factTypeID).setObjects(set(object)).setLastSeenTimestamp(DAY3));

    ObjectStatisticsContainer result = executeCalculateObjectStatistics(object.getId());
    assertEquals(1, getFirstStatistic(result, object.getId()).getFactCount());
  }

  private void assertSingleStatisticExists(ObjectStatisticsCriteria criteria, UUID objectID) {
    ObjectStatisticsContainer result = getFactSearchManager().calculateObjectStatistics(criteria);
    assertEquals(1, result.getStatisticsCount());
    assertNotNull(result.getStatistics(objectID));
  }

  private ObjectStatisticsContainer executeCalculateObjectStatistics(UUID... objectID) {
    ObjectStatisticsCriteria criteria = createObjectStatisticsCriteria(b -> b.setObjectID(set(objectID)));
    return getFactSearchManager().calculateObjectStatistics(criteria);
  }

  private ObjectStatisticsCriteria createObjectStatisticsCriteria(ObjectPreparation<ObjectStatisticsCriteria.Builder> preparation) {
    ObjectStatisticsCriteria.Builder builder = ObjectStatisticsCriteria.builder()
            .setAccessControlCriteria(createAccessControlCriteria())
            .setIndexSelectCriteria(createIndexSelectCriteria(DAY1, DAY3));
    if (preparation != null) {
      builder = preparation.prepare(builder);
    }
    return builder.build();
  }

  private UUID getFirstObjectID(FactDocument fact) {
    return first(fact.getObjects()).getId();
  }

  private ObjectStatisticsContainer.FactStatistic getFirstStatistic(ObjectStatisticsContainer result, UUID objectID) {
    return first(result.getStatistics(objectID));
  }

}
