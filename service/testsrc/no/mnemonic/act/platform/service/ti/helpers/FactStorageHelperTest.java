package no.mnemonic.act.platform.service.ti.helpers;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.entity.cassandra.AccessMode;
import no.mnemonic.act.platform.entity.cassandra.FactAclEntity;
import no.mnemonic.act.platform.entity.cassandra.FactCommentEntity;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;
import java.util.function.Supplier;

import static no.mnemonic.commons.testtools.MockitoTools.match;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactStorageHelperTest {

  @Mock
  private FactManager factManager;
  @Mock
  private Supplier<UUID> currentUserResolver;

  private FactStorageHelper helper;

  @Before
  public void initialize() {
    initMocks(this);
    helper = new FactStorageHelper(factManager, currentUserResolver);
  }

  @Test
  public void testSaveInitialAclSkipWithoutFact() {
    helper.saveInitialAclForNewFact(null, ListUtils.list(UUID.randomUUID()));
    verifyNoMoreInteractions(factManager);
  }

  @Test
  public void testSaveInitialAclSkipWhenAccessModePublic() {
    helper.saveInitialAclForNewFact(createFact().setAccessMode(AccessMode.Public), ListUtils.list(UUID.randomUUID()));
    verifyNoMoreInteractions(factManager);
  }

  @Test
  public void testSaveInitialAclWithAccessModeRoleBased() throws Exception {
    FactEntity fact = createFact().setAccessMode(AccessMode.RoleBased);
    UUID user = UUID.randomUUID();
    helper.saveInitialAclForNewFact(fact, ListUtils.list(user));
    verify(factManager).saveFactAclEntry(matchFactAclEntity(fact, user));
  }

  @Test
  public void testSaveInitialAclWithAccessModeExplicitAddsCurrentUser() throws Exception {
    FactEntity fact = createFact().setAccessMode(AccessMode.Explicit);
    UUID currentUser = UUID.randomUUID();
    when(currentUserResolver.get()).thenReturn(currentUser);
    helper.saveInitialAclForNewFact(fact, null);
    verify(factManager).saveFactAclEntry(matchFactAclEntity(fact, currentUser));
  }

  @Test
  public void testSaveAdditionalAclSkipWithoutFact() {
    helper.saveAdditionalAclForFact(null, ListUtils.list(UUID.randomUUID()));
    verifyNoMoreInteractions(factManager);
  }

  @Test
  public void testSaveAdditionalAclSkipWithoutAcl() {
    helper.saveAdditionalAclForFact(createFact(), null);
    verifyNoMoreInteractions(factManager);
  }

  @Test
  public void testSaveAdditionalAclSkipWhenAccessModePublic() {
    helper.saveAdditionalAclForFact(createFact().setAccessMode(AccessMode.Public), ListUtils.list(UUID.randomUUID()));
    verifyNoMoreInteractions(factManager);
  }

  @Test
  public void testSaveAdditionalAclSkipExisting() throws Exception {
    FactEntity fact = createFact().setAccessMode(AccessMode.Explicit);
    UUID user = UUID.randomUUID();
    when(factManager.fetchFactAcl(fact.getId())).thenReturn(ListUtils.list(new FactAclEntity().setSubjectID(user)));

    helper.saveAdditionalAclForFact(fact, ListUtils.list(user));
    verify(factManager).fetchFactAcl(fact.getId());
    verifyNoMoreInteractions(factManager);
  }

  @Test
  public void testSaveAdditionalAclEntries() throws Exception {
    FactEntity fact = createFact().setAccessMode(AccessMode.Explicit);
    UUID user = UUID.randomUUID();
    when(factManager.fetchFactAcl(fact.getId())).thenReturn(ListUtils.list(new FactAclEntity().setSubjectID(UUID.randomUUID())));

    helper.saveAdditionalAclForFact(fact, ListUtils.list(user));
    verify(factManager).fetchFactAcl(fact.getId());
    verify(factManager).saveFactAclEntry(matchFactAclEntity(fact, user));
  }

  @Test
  public void testSaveCommentSkipWithoutFact() {
    helper.saveCommentForFact(null, "Hello World!");
    verifyNoMoreInteractions(factManager);
  }

  @Test
  public void testSaveCommentSkipWithoutComment() {
    helper.saveCommentForFact(createFact(), null);
    verifyNoMoreInteractions(factManager);
  }

  @Test
  public void testSaveComment() throws Exception {
    FactEntity fact = createFact();
    String comment = "Hello World!";
    helper.saveCommentForFact(fact, comment);
    verify(factManager).saveFactComment(matchFactCommentEntity(fact, comment));
  }

  private FactEntity createFact() {
    return new FactEntity()
            .setId(UUID.randomUUID())
            .setSourceID(UUID.randomUUID());
  }

  private FactAclEntity matchFactAclEntity(FactEntity fact, UUID subjectID) {
    return match(entry -> {
      assertNotNull(entry.getId());
      assertEquals(fact.getId(), entry.getFactID());
      assertEquals(fact.getSourceID(), entry.getSourceID());
      assertEquals(subjectID, entry.getSubjectID());
      assertTrue(entry.getTimestamp() > 0);
      return true;
    });
  }

  private FactCommentEntity matchFactCommentEntity(FactEntity fact, String comment) {
    return match(entry -> {
      assertNotNull(entry.getId());
      assertEquals(fact.getId(), entry.getFactID());
      assertEquals(fact.getSourceID(), entry.getSourceID());
      assertEquals(comment, entry.getComment());
      assertTrue(entry.getTimestamp() > 0);
      assertNull(entry.getReplyToID());
      return true;
    });
  }

}
