package no.mnemonic.act.platform.dao.cassandra;

import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.cassandra.exceptions.ImmutableViolationException;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    assertNull(getFactManager().getFact(null));
    assertNull(getFactManager().getFact(UUID.randomUUID()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveFactWithNonExistingFactType() {
    getFactManager().saveFact(createFact());
  }

  @Test(expected = ImmutableViolationException.class)
  public void testSaveFactTwiceThrowsException() {
    FactEntity entity = createFact(createAndSaveFactType().getId());
    getFactManager().saveFact(entity);
    getFactManager().saveFact(entity);
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
  public void testFetchFactsById() {
    FactTypeEntity type = createAndSaveFactType();
    FactEntity expected = createAndSaveFact(type.getId(), "value");
    createAndSaveFact(type.getId(), "ignored");

    List<FactEntity> actual = ListUtils.list(getFactManager().getFacts(ListUtils.list(expected.getId())));
    assertEquals(1, actual.size());
    assertFact(expected, actual.get(0));
  }

  @Test
  public void testFetchFactsByIdWithUnknownId() {
    assertEquals(0, ListUtils.list(getFactManager().getFacts(null)).size());
    assertEquals(0, ListUtils.list(getFactManager().getFacts(ListUtils.list())).size());
    assertEquals(0, ListUtils.list(getFactManager().getFacts(ListUtils.list(UUID.randomUUID()))).size());
  }

  @Test
  public void testRefreshFact() {
    long timestamp = 123456789;
    FactManager manager = getFactManagerWithMockedClock(timestamp);
    FactEntity fact = createAndSaveFact();

    assertEquals(fact.getLastSeenTimestamp(), manager.getFact(fact.getId()).getLastSeenTimestamp());
    FactEntity refreshedFact = manager.refreshFact(fact.getId());
    assertEquals(fact.getId(), refreshedFact.getId());
    assertEquals(timestamp, refreshedFact.getLastSeenTimestamp());
    assertEquals(timestamp, manager.getFact(fact.getId()).getLastSeenTimestamp());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRefreshFactWithNonExistingFact() {
    getFactManager().refreshFact(UUID.randomUUID());
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

  private FactTypeEntity createFactType() {
    return createFactType("factType");
  }

  private FactTypeEntity createFactType(String name) {
    return new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setNamespaceID(UUID.randomUUID())
            .setName(name)
            .setValidator("validator")
            .setValidatorParameter("validatorParameter");
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
            .setSourceID(UUID.randomUUID())
            .setAccessMode(AccessMode.Public)
            .setConfidenceLevel(0)
            .setTimestamp(1)
            .setLastSeenTimestamp(2)
            .setBindings(Collections.singletonList(new FactEntity.FactObjectBinding()
                    .setObjectID(UUID.randomUUID())
                    .setDirection(Direction.BiDirectional)));
  }

  private FactAclEntity createFactAclEntry(UUID factID) {
    return new FactAclEntity()
            .setFactID(factID)
            .setId(UUID.randomUUID())
            .setSubjectID(UUID.randomUUID())
            .setSourceID(UUID.randomUUID())
            .setTimestamp(1);
  }

  private FactCommentEntity createFactComment(UUID factID) {
    return new FactCommentEntity()
            .setFactID(factID)
            .setId(UUID.randomUUID())
            .setReplyToID(UUID.randomUUID())
            .setSourceID(UUID.randomUUID())
            .setComment("Comment")
            .setTimestamp(1);
  }

  private MetaFactBindingEntity createMetaFactBinding(UUID factID) {
    return new MetaFactBindingEntity()
            .setFactID(factID)
            .setMetaFactID(UUID.randomUUID());
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
    assertEquals(expected.getSourceID(), actual.getSourceID());
    assertEquals(expected.getAccessMode(), actual.getAccessMode());
    assertEquals(expected.getConfidenceLevel(), actual.getConfidenceLevel());
    assertEquals(expected.getTimestamp(), actual.getTimestamp());
    assertEquals(expected.getLastSeenTimestamp(), actual.getLastSeenTimestamp());
    assertEquals(expected.getBindingsStored(), actual.getBindingsStored());
  }

  private void assertFactAclEntry(FactAclEntity expected, FactAclEntity actual) {
    assertEquals(expected.getFactID(), actual.getFactID());
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getSubjectID(), actual.getSubjectID());
    assertEquals(expected.getSourceID(), actual.getSourceID());
    assertEquals(expected.getTimestamp(), actual.getTimestamp());
  }

  private void assertFactComment(FactCommentEntity expected, FactCommentEntity actual) {
    assertEquals(expected.getFactID(), actual.getFactID());
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getReplyToID(), actual.getReplyToID());
    assertEquals(expected.getSourceID(), actual.getSourceID());
    assertEquals(expected.getComment(), actual.getComment());
    assertEquals(expected.getTimestamp(), actual.getTimestamp());
  }

  private void assertMetaFactBinding(MetaFactBindingEntity expected, MetaFactBindingEntity actual) {
    assertEquals(expected.getFactID(), actual.getFactID());
    assertEquals(expected.getMetaFactID(), actual.getMetaFactID());
  }

  private FactManager getFactManagerWithMockedClock(long timestamp) {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.ofEpochMilli(timestamp));
    return getFactManager().withClock(clock);
  }

}
