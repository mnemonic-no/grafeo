package no.mnemonic.act.platform.dao.cassandra;

import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.cassandra.exceptions.ImmutableViolationException;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.Assert.*;

public class FactManagerTest extends AbstractManagerTest {

  @Test
  public void testSaveAndGetFactTypeById() {
    FactTypeEntity entity = createAndSaveFactType();
    assertFactType(entity, getFactManager().getFactType(entity.getId()));
  }

  @Test
  public void testGetFactTypeWithUnknownIdReturnsNull() {
    assertNull(getFactManager().getFactType((UUID) null));
    assertNull(getFactManager().getFactType(UUID.randomUUID()));
  }

  @Test
  public void testGetFactTypeByIdTwiceReturnsSameInstance() {
    FactTypeEntity entity = createAndSaveFactType();
    FactTypeEntity type1 = getFactManager().getFactType(entity.getId());
    FactTypeEntity type2 = getFactManager().getFactType(entity.getId());
    assertSame(type1, type2);
  }

  @Test
  public void testSaveFactTypeTwiceInvalidatesIdCache() {
    FactTypeEntity entity = createFactType();
    getFactManager().saveFactType(entity);
    FactTypeEntity type1 = getFactManager().getFactType(entity.getId());
    getFactManager().saveFactType(entity);
    FactTypeEntity type2 = getFactManager().getFactType(entity.getId());
    assertNotSame(type1, type2);
  }

  @Test
  public void testSaveAndGetFactTypeByName() {
    FactTypeEntity entity = createAndSaveFactType();
    assertFactType(entity, getFactManager().getFactType(entity.getName()));
  }

  @Test
  public void testGetFactTypeWithUnknownNameReturnsNull() {
    assertNull(getFactManager().getFactType((String) null));
    assertNull(getFactManager().getFactType(""));
    assertNull(getFactManager().getFactType("Unknown"));
  }

  @Test
  public void testGetFactTypeByNameTwiceReturnsSameInstance() {
    FactTypeEntity entity = createAndSaveFactType();
    FactTypeEntity type1 = getFactManager().getFactType(entity.getName());
    FactTypeEntity type2 = getFactManager().getFactType(entity.getName());
    assertSame(type1, type2);
  }

  @Test
  public void testSaveFactTypeTwiceInvalidatesNameCache() {
    FactTypeEntity entity = createFactType();
    getFactManager().saveFactType(entity);
    FactTypeEntity type1 = getFactManager().getFactType(entity.getName());
    getFactManager().saveFactType(entity);
    FactTypeEntity type2 = getFactManager().getFactType(entity.getName());
    assertNotSame(type1, type2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveFactTypeWithSameNameThrowsException() {
    getFactManager().saveFactType(createFactType("factType"));
    getFactManager().saveFactType(createFactType("factType"));
  }

  @Test
  public void testSaveFactTypeReturnsSameEntity() {
    FactTypeEntity entity = createFactType();
    assertSame(entity, getFactManager().saveFactType(entity));
  }

  @Test
  public void testSaveFactTypeReturnsNullOnNullInput() {
    assertNull(getFactManager().saveFactType(null));
  }

  @Test
  public void testSaveFactTypeAvoidsDuplicateRelevantObjectBindings() {
    UUID sourceObjectTypeID = UUID.randomUUID();
    UUID destinationObjectTypeID = UUID.randomUUID();

    FactTypeEntity entity = createFactType();
    entity.setRelevantObjectBindings(SetUtils.set(
            createBindingDefinition(sourceObjectTypeID, destinationObjectTypeID),
            createBindingDefinition(sourceObjectTypeID, destinationObjectTypeID)
    ));
    getFactManager().saveFactType(entity);
    assertEquals(1, getFactManager().getFactType(entity.getId()).getRelevantObjectBindings().size());
  }

  @Test
  public void testSaveFactTypeAvoidsDuplicateRelevantFactBindings() {
    UUID factTypeID = UUID.randomUUID();

    FactTypeEntity entity = createFactType();
    entity.setRelevantFactBindings(SetUtils.set(
            new FactTypeEntity.MetaFactBindingDefinition().setFactTypeID(factTypeID),
            new FactTypeEntity.MetaFactBindingDefinition().setFactTypeID(factTypeID)
    ));
    getFactManager().saveFactType(entity);
    assertEquals(1, getFactManager().getFactType(entity.getId()).getRelevantFactBindings().size());
  }

  @Test
  public void testSaveFactTypeWithDefaultValue() {
    UUID factTypeID = UUID.randomUUID();

    getFactManager().saveFactType(new FactTypeEntity()
            .setId(factTypeID)
            .setName("factType")
    );
    FactTypeEntity entity = getFactManager().getFactType(factTypeID);
    assertEquals(FactTypeEntity.DEFAULT_CONFIDENCE, entity.getDefaultConfidence(), 0);
  }

  @Test
  public void testFetchFactTypes() {
    List<FactTypeEntity> expected = createAndSaveFactTypes(3);
    List<FactTypeEntity> actual = getFactManager().fetchFactTypes();

    expected.sort(Comparator.comparing(FactTypeEntity::getId));
    actual.sort(Comparator.comparing(FactTypeEntity::getId));

    assertFactTypes(expected, actual);
  }

  @Test
  public void testSaveAndGetFact() {
    FactEntity entity = createAndSaveFact();
    assertFact(entity, getFactManager().getFact(entity.getId()));
  }

  @Test
  public void testGetFactWithNonExistingFact() {
    assertNull(getFactManager().getFact((UUID) null));
    assertNull(getFactManager().getFact(UUID.randomUUID()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveFactWithNonExistingFactType() {
    getFactManager().saveFact(createFact());
  }

  @Test
  public void testSaveFactReturnsSameEntity() {
    FactEntity entity = createFact(createAndSaveFactType().getId());
    assertSame(entity, getFactManager().saveFact(entity));
  }

  @Test
  public void testSaveFactReturnsNullOnNullInput() {
    assertNull(getFactManager().saveFact(null));
  }

  @Test
  public void testSaveFactWithDefaultValues() {
    UUID factID = UUID.randomUUID();
    UUID factTypeID = createAndSaveFactType().getId();

    getFactManager().saveFact(new FactEntity()
            .setId(factID)
            .setTypeID(factTypeID)
    );
    FactEntity entity = getFactManager().getFact(factID);
    assertEquals(FactEntity.DEFAULT_CONFIDENCE, entity.getConfidence(), 0);
    assertEquals(FactEntity.DEFAULT_TRUST, entity.getTrust(), 0);
  }

  @Test
  public void testSaveFactWithMultipleFlags() {
    UUID factID = UUID.randomUUID();
    UUID factTypeID = createAndSaveFactType().getId();

    getFactManager().saveFact(new FactEntity()
            .setId(factID)
            .setTypeID(factTypeID)
            .addFlag(FactEntity.Flag.RetractedHint)
            .addFlag(FactEntity.Flag.HasComments)
    );
    FactEntity entity = getFactManager().getFact(factID);
    assertTrue(entity.getFlags().contains(FactEntity.Flag.RetractedHint));
    assertTrue(entity.getFlags().contains(FactEntity.Flag.HasComments));
  }

  @Test
  public void testGetFactByHashReturnsNullOnInvalidInput() {
    assertNull(getFactManager().getFact((String) null));
    assertNull(getFactManager().getFact(""));
    assertNull(getFactManager().getFact(" "));
    assertNull(getFactManager().getFact("something"));
  }

  @Test
  public void testGetFactByHashReturnsFact() {
    FactEntity expected = createAndSaveFact();
    FactExistenceEntity existenceEntity = createFactExistence(expected.getId());
    getFactManager().saveFactExistence(existenceEntity);

    FactEntity actual = getFactManager().getFact(existenceEntity.getFactHash());
    assertNotNull(actual);
    assertEquals(expected.getId(), actual.getId());
  }

  @Test
  public void testFetchFactsWithinTimeframeSingleBucket() {
    long timestamp = 1609504200000L;

    FactTypeEntity type = createAndSaveFactType();
    FactEntity expected = createAndSaveFactWithTimestamp(type, timestamp);

    List<UUID> actual = ListUtils.list(getFactManager().getFactsWithin(timestamp - 1000, timestamp + 1000), FactEntity::getId);
    assertEquals(ListUtils.list(expected.getId()), actual);
  }

  @Test
  public void testFetchFactsWithinTimeframeSingleBucketOutsideTimeframe() {
    long timestamp1 = 1609504200000L;
    long timestamp2 = timestamp1 - 2000;
    long timestamp3 = timestamp1 + 2000;

    FactTypeEntity type = createAndSaveFactType();
    FactEntity expected = createAndSaveFactWithTimestamp(type, timestamp1);
    createAndSaveFactWithTimestamp(type, timestamp2);
    createAndSaveFactWithTimestamp(type, timestamp3);

    List<UUID> actual = ListUtils.list(getFactManager().getFactsWithin(timestamp1 - 1000, timestamp1 + 1000), FactEntity::getId);
    assertEquals(ListUtils.list(expected.getId()), actual);
  }

  @Test
  public void testFetchFactsWithinTimeframeMultipleBuckets() {
    long timestamp1 = 1609500600000L;
    long timestamp2 = 1609504200000L;
    long timestamp3 = 1609507800000L;

    FactTypeEntity type = createAndSaveFactType();
    FactEntity fact1 = createAndSaveFactWithTimestamp(type, timestamp1);
    FactEntity fact2 = createAndSaveFactWithTimestamp(type, timestamp2);
    FactEntity fact3 = createAndSaveFactWithTimestamp(type, timestamp3);

    List<UUID> actual = ListUtils.list(getFactManager().getFactsWithin(timestamp1 - 1000, timestamp3 + 1000), FactEntity::getId);
    assertEquals(ListUtils.list(fact1.getId(), fact2.getId(), fact3.getId()), actual);
  }

  @Test
  public void testFetchFactsWithinTimeframeMultipleBucketsOutsideTimeframe() {
    long timestamp1 = 1609500600000L;
    long timestamp2 = 1609504200000L;
    long timestamp3 = 1609507800000L;

    FactTypeEntity type = createAndSaveFactType();
    createAndSaveFactWithTimestamp(type, timestamp1);
    FactEntity expected = createAndSaveFactWithTimestamp(type, timestamp2);
    createAndSaveFactWithTimestamp(type, timestamp3);

    List<UUID> actual = ListUtils.list(getFactManager().getFactsWithin(timestamp2 - 1000, timestamp2 + 1000), FactEntity::getId);
    assertEquals(ListUtils.list(expected.getId()), actual);
  }

  @Test
  public void testFetchFactsWithinTimeframeMultipleBucketsSkipsEmtpyBucket() {
    long timestamp1 = 1609500600000L;
    long timestamp2 = 1609507800000L;

    FactTypeEntity type = createAndSaveFactType();
    FactEntity fact1 = createAndSaveFactWithTimestamp(type, timestamp1);
    FactEntity fact2 = createAndSaveFactWithTimestamp(type, timestamp2);

    List<UUID> actual = ListUtils.list(getFactManager().getFactsWithin(timestamp1 - 1000, timestamp2 + 1000), FactEntity::getId);
    assertEquals(ListUtils.list(fact1.getId(), fact2.getId()), actual);
  }

  @Test
  public void testFetchFactsWithinTimeframeWithoutFacts() {
    assertTrue(ListUtils.list(getFactManager().getFactsWithin(1609500600000L, 1609507800000L)).isEmpty());
  }

  @Test
  public void testFetchFactsWithinTimeframeWithInvalidTimestamp() {
    assertThrows(IllegalArgumentException.class, () -> getFactManager().getFactsWithin(-1, 1));
    assertThrows(IllegalArgumentException.class, () -> getFactManager().getFactsWithin(1, -1));
    assertThrows(IllegalArgumentException.class, () -> getFactManager().getFactsWithin(2, 1));
  }

  @Test
  public void testSaveAndFetchFactAcl() {
    FactEntity fact = createAndSaveFact();
    FactAclEntity entry = createAndSaveFactAclEntry(fact.getId());
    List<FactAclEntity> acl = getFactManager().fetchFactAcl(fact.getId());

    assertEquals(1, acl.size());
    assertFactAclEntry(entry, acl.get(0));
  }

  @Test
  public void testFetchFactAclWithNonExistingFact() {
    assertEquals(0, getFactManager().fetchFactAcl(null).size());
    assertEquals(0, getFactManager().fetchFactAcl(UUID.randomUUID()).size());
  }

  @Test
  public void testSaveFactAclEntryReturnsSameEntity() {
    FactAclEntity entity = createFactAclEntry(createAndSaveFact().getId());
    assertSame(entity, getFactManager().saveFactAclEntry(entity));
  }

  @Test
  public void testSaveFactAclEntryReturnsNullOnNullInput() {
    assertNull(getFactManager().saveFactAclEntry(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveFactAclEntryWithNonExistingFactThrowsException() {
    getFactManager().saveFactAclEntry(createFactAclEntry(UUID.randomUUID()));
  }

  @Test(expected = ImmutableViolationException.class)
  public void testSaveFactAclEntryTwiceThrowsException() {
    FactAclEntity entry = createFactAclEntry(createAndSaveFact().getId());
    getFactManager().saveFactAclEntry(entry);
    getFactManager().saveFactAclEntry(entry);
  }

  @Test
  public void testSaveAndFetchFactComments() {
    FactEntity fact = createAndSaveFact();
    FactCommentEntity comment = createAndSaveFactComment(fact.getId());
    List<FactCommentEntity> comments = getFactManager().fetchFactComments(fact.getId());

    assertEquals(1, comments.size());
    assertFactComment(comment, comments.get(0));
  }

  @Test
  public void testFetchFactCommentsWithNonExistingFact() {
    assertEquals(0, getFactManager().fetchFactComments(null).size());
    assertEquals(0, getFactManager().fetchFactComments(UUID.randomUUID()).size());
  }

  @Test
  public void testSaveFactCommentReturnsSameEntity() {
    FactCommentEntity entity = createFactComment(createAndSaveFact().getId());
    assertSame(entity, getFactManager().saveFactComment(entity));
  }

  @Test
  public void testSaveFactCommentReturnsNullOnNullInput() {
    assertNull(getFactManager().saveFactComment(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveFactCommentWithNonExistingFactThrowsException() {
    getFactManager().saveFactComment(createFactComment(UUID.randomUUID()));
  }

  @Test(expected = ImmutableViolationException.class)
  public void testSaveFactCommentTwiceThrowsException() {
    FactCommentEntity comment = createFactComment(createAndSaveFact().getId());
    getFactManager().saveFactComment(comment);
    getFactManager().saveFactComment(comment);
  }

  @Test
  public void testSaveAndFetchMetaFactBindings() {
    FactEntity fact = createAndSaveFact();
    MetaFactBindingEntity binding = createAndSaveMetaFactBinding(fact.getId());

    List<MetaFactBindingEntity> actual = getFactManager().fetchMetaFactBindings(fact.getId());
    assertEquals(1, actual.size());
    assertMetaFactBinding(binding, actual.get(0));
  }

  @Test
  public void testFetchMetaFactBindingsWithNonExistingFact() {
    assertEquals(0, getFactManager().fetchMetaFactBindings(null).size());
    assertEquals(0, getFactManager().fetchMetaFactBindings(UUID.randomUUID()).size());
  }

  @Test
  public void testSaveMetaFactBindingReturnsSameEntity() {
    MetaFactBindingEntity binding = createMetaFactBinding(createAndSaveFact().getId());
    assertSame(binding, getFactManager().saveMetaFactBinding(binding));
  }

  @Test
  public void testSaveMetaFactBindingReturnsNullOnNullInput() {
    assertNull(getFactManager().saveMetaFactBinding(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveMetaFactBindingWithNonExistingFactThrowsException() {
    getFactManager().saveMetaFactBinding(createMetaFactBinding(UUID.randomUUID()));
  }

  @Test(expected = ImmutableViolationException.class)
  public void testSaveMetaFactBindingTwiceThrowsException() {
    MetaFactBindingEntity binding = createMetaFactBinding(createAndSaveFact().getId());
    getFactManager().saveMetaFactBinding(binding);
    getFactManager().saveMetaFactBinding(binding);
  }

  @Test
  public void testSaveFactByTimestampReturnsSameEntity() {
    FactByTimestampEntity entity = createFactByTimestamp(createAndSaveFact().getId());
    assertSame(entity, getFactManager().saveFactByTimestamp(entity));
  }

  @Test
  public void testSaveFactByTimestampReturnsNullOnNullInput() {
    assertNull(getFactManager().saveFactByTimestamp(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveFactByTimestampWithNonExistingFactThrowsException() {
    getFactManager().saveFactByTimestamp(createFactByTimestamp(UUID.randomUUID()));
  }

  @Test(expected = ImmutableViolationException.class)
  public void testSaveFactByTimestampTwiceThrowsException() {
    FactByTimestampEntity entity = createFactByTimestamp(createAndSaveFact().getId());
    getFactManager().saveFactByTimestamp(entity);
    getFactManager().saveFactByTimestamp(entity);
  }

  @Test
  public void testSaveFactExistenceReturnsSameEntity() {
    FactExistenceEntity entity = createFactExistence(createAndSaveFact().getId());
    assertSame(entity, getFactManager().saveFactExistence(entity));
  }

  @Test
  public void testSaveFactExistenceReturnsNullOnNullInput() {
    assertNull(getFactManager().saveFactExistence(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveFactExistenceWithNonExistingFactThrowsException() {
    getFactManager().saveFactExistence(createFactExistence(UUID.randomUUID()));
  }

  @Test(expected = ImmutableViolationException.class)
  public void testSaveFactExistenceTwiceThrowsException() {
    FactExistenceEntity entity = createFactExistence(createAndSaveFact().getId());
    getFactManager().saveFactExistence(entity);
    getFactManager().saveFactExistence(entity);
  }

  private FactTypeEntity createFactType() {
    return createFactType("factType");
  }

  private FactTypeEntity createFactType(String name) {
    return new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setNamespaceID(UUID.randomUUID())
            .setName(name)
            .setValidator("validator")
            .setValidatorParameter("validatorParameter")
            .setDefaultConfidence(0.5f);
  }

  private FactTypeEntity.FactObjectBindingDefinition createBindingDefinition(UUID sourceObjectTypeID, UUID destinationObjectTypeID) {
    return new FactTypeEntity.FactObjectBindingDefinition()
            .setSourceObjectTypeID(sourceObjectTypeID)
            .setDestinationObjectTypeID(destinationObjectTypeID)
            .setBidirectionalBinding(true);
  }

  private FactEntity createFact() {
    return createFact(UUID.randomUUID());
  }

  private FactEntity createFact(UUID typeID) {
    return createFact(typeID, "value");
  }

  private FactEntity createFact(UUID typeID, String value) {
    return new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(typeID)
            .setValue(value)
            .setInReferenceToID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setAddedByID(UUID.randomUUID())
            .setAccessMode(AccessMode.Public)
            .setConfidence(0.1f)
            .setTrust(0.2f)
            .setTimestamp(1)
            .setLastSeenTimestamp(2)
            .setSourceObjectID(UUID.randomUUID())
            .setDestinationObjectID(UUID.randomUUID())
            .setBindings(Collections.singletonList(new FactEntity.FactObjectBinding()
                    .setObjectID(UUID.randomUUID())
                    .setDirection(Direction.BiDirectional)));
  }

  private FactAclEntity createFactAclEntry(UUID factID) {
    return new FactAclEntity()
            .setFactID(factID)
            .setId(UUID.randomUUID())
            .setSubjectID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setTimestamp(1);
  }

  private FactCommentEntity createFactComment(UUID factID) {
    return new FactCommentEntity()
            .setFactID(factID)
            .setId(UUID.randomUUID())
            .setReplyToID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setComment("Comment")
            .setTimestamp(1);
  }

  private MetaFactBindingEntity createMetaFactBinding(UUID factID) {
    return new MetaFactBindingEntity()
            .setFactID(factID)
            .setMetaFactID(UUID.randomUUID());
  }

  private FactByTimestampEntity createFactByTimestamp(UUID factID) {
    return new FactByTimestampEntity()
            .setHourOfDay(1609502400000L)
            .setTimestamp(1609504200000L)
            .setFactID(factID);
  }

  private FactExistenceEntity createFactExistence(UUID factID) {
    return new FactExistenceEntity()
            .setFactHash("abc789")
            .setFactID(factID);
  }

  private FactTypeEntity createAndSaveFactType() {
    return createAndSaveFactTypes(1).get(0);
  }

  private List<FactTypeEntity> createAndSaveFactTypes(int numberOfEntities) {
    List<FactTypeEntity> entities = new ArrayList<>();

    for (int i = 0; i < numberOfEntities; i++) {
      entities.add(getFactManager().saveFactType(createFactType("factType" + i)));
    }

    return entities;
  }

  private FactEntity createAndSaveFact() {
    return createAndSaveFact(createAndSaveFactType().getId(), "value");
  }

  private FactEntity createAndSaveFact(UUID typeID, String value) {
    return getFactManager().saveFact(createFact(typeID, value));
  }

  private FactEntity createAndSaveFactWithTimestamp(FactTypeEntity type, long timestamp) {
    FactEntity fact = createFact(type.getId())
            .setTimestamp(timestamp);

    getFactManager().saveFact(fact);
    getFactManager().saveFactByTimestamp(new FactByTimestampEntity()
            .setHourOfDay(Instant.ofEpochMilli(timestamp).truncatedTo(ChronoUnit.HOURS).toEpochMilli())
            .setTimestamp(timestamp)
            .setFactID(fact.getId()));

    return fact;
  }

  private FactAclEntity createAndSaveFactAclEntry(UUID factID) {
    return getFactManager().saveFactAclEntry(createFactAclEntry(factID));
  }

  private FactCommentEntity createAndSaveFactComment(UUID factID) {
    return getFactManager().saveFactComment(createFactComment(factID));
  }

  private MetaFactBindingEntity createAndSaveMetaFactBinding(UUID factID) {
    return getFactManager().saveMetaFactBinding(createMetaFactBinding(factID));
  }

  private void assertFactType(FactTypeEntity expected, FactTypeEntity actual) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getNamespaceID(), actual.getNamespaceID());
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getValidator(), actual.getValidator());
    assertEquals(expected.getValidatorParameter(), actual.getValidatorParameter());
    assertEquals(expected.getRelevantObjectBindingsStored(), actual.getRelevantObjectBindingsStored());
    assertEquals(expected.getDefaultConfidence(), actual.getDefaultConfidence(), 0);
  }

  private void assertFactTypes(List<FactTypeEntity> expected, List<FactTypeEntity> actual) {
    assertEquals(expected.size(), actual.size());
    for (int i = 0; i < expected.size(); i++) {
      assertFactType(expected.get(i), actual.get(i));
    }
  }

  private void assertFact(FactEntity expected, FactEntity actual) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getTypeID(), actual.getTypeID());
    assertEquals(expected.getValue(), actual.getValue());
    assertEquals(expected.getInReferenceToID(), actual.getInReferenceToID());
    assertEquals(expected.getOrganizationID(), actual.getOrganizationID());
    assertEquals(expected.getOriginID(), actual.getOriginID());
    assertEquals(expected.getAddedByID(), actual.getAddedByID());
    assertEquals(expected.getAccessMode(), actual.getAccessMode());
    assertEquals(expected.getConfidence(), actual.getConfidence(), 0);
    assertEquals(expected.getTrust(), actual.getTrust(), 0);
    assertEquals(expected.getTimestamp(), actual.getTimestamp());
    assertEquals(expected.getLastSeenTimestamp(), actual.getLastSeenTimestamp());
    assertEquals(expected.getSourceObjectID(), actual.getSourceObjectID());
    assertEquals(expected.getDestinationObjectID(), actual.getDestinationObjectID());
    assertEquals(expected.getBindingsStored(), actual.getBindingsStored());
  }

  private void assertFactAclEntry(FactAclEntity expected, FactAclEntity actual) {
    assertEquals(expected.getFactID(), actual.getFactID());
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getSubjectID(), actual.getSubjectID());
    assertEquals(expected.getOriginID(), actual.getOriginID());
    assertEquals(expected.getTimestamp(), actual.getTimestamp());
  }

  private void assertFactComment(FactCommentEntity expected, FactCommentEntity actual) {
    assertEquals(expected.getFactID(), actual.getFactID());
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getReplyToID(), actual.getReplyToID());
    assertEquals(expected.getOriginID(), actual.getOriginID());
    assertEquals(expected.getComment(), actual.getComment());
    assertEquals(expected.getTimestamp(), actual.getTimestamp());
  }

  private void assertMetaFactBinding(MetaFactBindingEntity expected, MetaFactBindingEntity actual) {
    assertEquals(expected.getFactID(), actual.getFactID());
    assertEquals(expected.getMetaFactID(), actual.getMetaFactID());
  }

}
