package no.mnemonic.act.platform.dao.cassandra;

import no.mnemonic.act.platform.dao.cassandra.exceptions.ImmutableViolationException;
import no.mnemonic.act.platform.entity.cassandra.*;
import org.junit.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FactManagerTest extends AbstractManagerTest {

  @Test
  public void testSaveAndGetFactTypeById() {
    FactTypeEntity entity = createAndSaveFactType();
    assertFactType(entity, getFactManager().getFactType(entity.getId()));
  }

  @Test
  public void testGetFactTypeWithUnknownIdReturnsNull() {
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
  public void testFetchFactTypes() {
    List<FactTypeEntity> expected = createAndSaveFactTypes(3);
    List<FactTypeEntity> actual = getFactManager().fetchFactTypes();

    Comparator<FactTypeEntity> comparator = (e1, e2) -> e1.getId().compareTo(e2.getId());
    expected.sort(comparator);
    actual.sort(comparator);

    assertFactTypes(expected, actual);
  }

  @Test
  public void testSaveAndGetFact() throws Exception {
    FactEntity entity = createAndSaveFact();
    assertFact(entity, getFactManager().getFact(entity.getId()));
  }

  @Test
  public void testGetFactWithNonExistingFact() throws ImmutableViolationException {
    assertNull(getFactManager().getFact(UUID.randomUUID()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveFactWithNonExistingFactType() throws Exception {
    getFactManager().saveFact(createFact());
  }

  @Test(expected = ImmutableViolationException.class)
  public void testSaveFactTwiceThrowsException() throws Exception {
    FactEntity entity = createFact(createAndSaveFactType().getId());
    getFactManager().saveFact(entity);
    getFactManager().saveFact(entity);
  }

  @Test
  public void testSaveFactReturnsSameEntity() throws Exception {
    FactEntity entity = createFact(createAndSaveFactType().getId());
    assertSame(entity, getFactManager().saveFact(entity));
  }

  @Test
  public void testSaveFactReturnsNullOnNullInput() throws ImmutableViolationException {
    assertNull(getFactManager().saveFact(null));
  }

  @Test
  public void testEncodeWhenSavingFact() throws Exception {
    createAndSaveFact();
    verify(getEntityHandler(), times(1)).encode(any());
  }

  @Test
  public void testDecodeWhenRetrievingFact() throws Exception {
    getFactManager().getFact(createAndSaveFact().getId());
    verify(getEntityHandler(), times(1)).decode(any());
  }

  @Test
  public void testRefreshFact() throws Exception {
    long timestamp = 123456789;
    FactManager manager = getFactManagerWithMockedClock(timestamp);
    FactEntity fact = createAndSaveFact();

    assertEquals(fact.getLastSeenTimestamp(), manager.getFact(fact.getId()).getLastSeenTimestamp());
    manager.refreshFact(fact.getId());
    assertEquals(timestamp, manager.getFact(fact.getId()).getLastSeenTimestamp());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRefreshFactWithNonExistingFact() throws Exception {
    getFactManager().refreshFact(UUID.randomUUID());
  }

  @Test
  public void testSaveAndFetchFactAcl() throws Exception {
    FactEntity fact = createAndSaveFact();
    FactAclEntity entry = createAndSaveFactAclEntry(fact.getId());
    List<FactAclEntity> acl = getFactManager().fetchFactAcl(fact.getId());

    assertEquals(1, acl.size());
    assertFactAclEntry(entry, acl.get(0));
  }

  @Test
  public void testFetchFactAclWithNonExistingFact() {
    assertEquals(0, getFactManager().fetchFactAcl(UUID.randomUUID()).size());
  }

  @Test
  public void testSaveFactAclEntryReturnsSameEntity() throws Exception {
    FactAclEntity entity = createFactAclEntry(createAndSaveFact().getId());
    assertSame(entity, getFactManager().saveFactAclEntry(entity));
  }

  @Test
  public void testSaveFactAclEntryReturnsNullOnNullInput() throws ImmutableViolationException {
    assertNull(getFactManager().saveFactAclEntry(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveFactAclEntryWithNonExistingFactThrowsException() throws Exception {
    getFactManager().saveFactAclEntry(createFactAclEntry(UUID.randomUUID()));
  }

  @Test(expected = ImmutableViolationException.class)
  public void testSaveFactAclEntryTwiceThrowsException() throws Exception {
    FactAclEntity entry = createFactAclEntry(createAndSaveFact().getId());
    getFactManager().saveFactAclEntry(entry);
    getFactManager().saveFactAclEntry(entry);
  }

  @Test
  public void testSaveAndFetchFactComments() throws Exception {
    FactEntity fact = createAndSaveFact();
    FactCommentEntity comment = createAndSaveFactComment(fact.getId());
    List<FactCommentEntity> comments = getFactManager().fetchFactComments(fact.getId());

    assertEquals(1, comments.size());
    assertFactComment(comment, comments.get(0));
  }

  @Test
  public void testFetchFactCommentsWithNonExistingFact() {
    assertEquals(0, getFactManager().fetchFactComments(UUID.randomUUID()).size());
  }

  @Test
  public void testSaveFactCommentReturnsSameEntity() throws Exception {
    FactCommentEntity entity = createFactComment(createAndSaveFact().getId());
    assertSame(entity, getFactManager().saveFactComment(entity));
  }

  @Test
  public void testSaveFactCommentReturnsNullOnNullInput() throws ImmutableViolationException {
    assertNull(getFactManager().saveFactComment(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveFactCommentWithNonExistingFactThrowsException() throws Exception {
    getFactManager().saveFactComment(createFactComment(UUID.randomUUID()));
  }

  @Test(expected = ImmutableViolationException.class)
  public void testSaveFactCommentTwiceThrowsException() throws Exception {
    FactCommentEntity comment = createFactComment(createAndSaveFact().getId());
    getFactManager().saveFactComment(comment);
    getFactManager().saveFactComment(comment);
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
            .setEntityHandler("entityHandler")
            .setEntityHandlerParameter("entityHandlerParameter");
  }

  private FactEntity createFact() throws IOException {
    return createFact(UUID.randomUUID());
  }

  private FactEntity createFact(UUID typeID) throws IOException {
    return new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(typeID)
            .setValue("value")
            .setInReferenceToID(UUID.randomUUID())
            .setCustomerID(UUID.randomUUID())
            .setSourceID(UUID.randomUUID())
            .setAccessMode(AccessMode.Public)
            .setConfidenceLevel(0)
            .setTimestamp(1)
            .setLastSeenTimestamp(2)
            .setBindings(Collections.singletonList(createFactObjectBinding(Direction.None)));
  }

  private FactEntity.FactObjectBinding createFactObjectBinding(Direction direction) {
    return new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(direction);
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

  private FactEntity createAndSaveFact() throws Exception {
    return createAndSaveFact(createAndSaveFactType().getId());
  }

  private FactEntity createAndSaveFact(UUID typeID) throws Exception {
    return getFactManager().saveFact(createFact(typeID));
  }

  private FactAclEntity createAndSaveFactAclEntry(UUID factID) throws ImmutableViolationException {
    return getFactManager().saveFactAclEntry(createFactAclEntry(factID));
  }

  private FactCommentEntity createAndSaveFactComment(UUID factID) throws ImmutableViolationException {
    return getFactManager().saveFactComment(createFactComment(factID));
  }

  private void assertFactType(FactTypeEntity expected, FactTypeEntity actual) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getNamespaceID(), actual.getNamespaceID());
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getValidator(), actual.getValidator());
    assertEquals(expected.getValidatorParameter(), actual.getValidatorParameter());
    assertEquals(expected.getEntityHandler(), actual.getEntityHandler());
    assertEquals(expected.getEntityHandlerParameter(), actual.getEntityHandlerParameter());
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
    assertEquals(expected.getCustomerID(), actual.getCustomerID());
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

  private FactManager getFactManagerWithMockedClock(long timestamp) {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.ofEpochMilli(timestamp));
    return getFactManager().withClock(clock);
  }

}
