package no.mnemonic.act.platform.service.ti.helpers;

import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import java.util.UUID;

import static no.mnemonic.act.platform.service.ti.helpers.FactHelper.withAcl;
import static no.mnemonic.act.platform.service.ti.helpers.FactHelper.withComment;
import static org.junit.Assert.*;

public class FactHelperTest {

  @Test
  public void testWithCommentNullFact() {
    assertNull(withComment(null, "Hello World!"));
  }

  @Test
  public void testWithCommentSkipsBlankComment() {
    assertTrue(CollectionUtils.isEmpty(withComment(new FactRecord(), null).getComments()));
    assertTrue(CollectionUtils.isEmpty(withComment(new FactRecord(), "").getComments()));
    assertTrue(CollectionUtils.isEmpty(withComment(new FactRecord(), " ").getComments()));
  }

  @Test
  public void testWithCommentAddsAdditionalComment() {
    FactRecord fact = new FactRecord()
      .setOriginID(UUID.randomUUID());

    fact = withComment(fact, "Hello World!");

    assertEquals(1, fact.getComments().size());
    assertNotNull(fact.getComments().get(0).getId());
    assertEquals(fact.getOriginID(), fact.getComments().get(0).getOriginID());
    assertEquals("Hello World!", fact.getComments().get(0).getComment());
    assertTrue(fact.getComments().get(0).getTimestamp() > 0);
  }

  @Test
  public void testWithAclNullFact() {
    assertNull(withAcl(null, UUID.randomUUID(), ListUtils.list(UUID.randomUUID())));
  }

  @Test
  public void testWithAclSkipsEmptyAcl() {
    assertTrue(CollectionUtils.isEmpty(withAcl(new FactRecord(), UUID.randomUUID(), null).getAcl()));
    assertTrue(CollectionUtils.isEmpty(withAcl(new FactRecord(), UUID.randomUUID(), ListUtils.list()).getAcl()));
  }

  @Test
  public void testWithAclAddsEntryToEmptyAcl() {
    UUID subjectID = UUID.randomUUID();
    FactRecord fact = new FactRecord().setOriginID(UUID.randomUUID());

    fact = withAcl(fact, UUID.randomUUID(), ListUtils.list(subjectID));
    assertEquals(1, fact.getAcl().size());
    assertNotNull(fact.getAcl().get(0).getId());
    assertEquals(fact.getOriginID(), fact.getAcl().get(0).getOriginID());
    assertEquals(subjectID, fact.getAcl().get(0).getSubjectID());
    assertTrue(fact.getAcl().get(0).getTimestamp() > 0);
  }

  @Test
  public void testWithAclAddsEntryToExistingAcl() {
    UUID subjectID = UUID.randomUUID();
    FactRecord fact = new FactRecord().addAclEntry(new FactAclEntryRecord().setSubjectID(UUID.randomUUID()));

    fact = withAcl(fact, UUID.randomUUID(), ListUtils.list(subjectID));
    assertEquals(2, fact.getAcl().size());
  }

  @Test
  public void testWithAclSkipsExistingEntry() {
    UUID subjectID = UUID.randomUUID();
    FactRecord fact = new FactRecord().addAclEntry(new FactAclEntryRecord().setSubjectID(subjectID));

    fact = withAcl(fact, UUID.randomUUID(), ListUtils.list(subjectID));
    assertEquals(1, fact.getAcl().size());
  }

  @Test
  public void testWithAclAccessModeExplicitAddsCurrentUser() {
    UUID currentUser = UUID.randomUUID();
    FactRecord fact = new FactRecord().setAccessMode(FactRecord.AccessMode.Explicit);

    fact = withAcl(fact, currentUser, null);
    assertEquals(1, fact.getAcl().size());
    assertEquals(currentUser, fact.getAcl().get(0).getSubjectID());
  }

  @Test
  public void testWithAclAccessModeExplicitSkipsExistingCurrentUser() {
    UUID currentUser = UUID.randomUUID();
    FactRecord fact = new FactRecord()
      .setAccessMode(FactRecord.AccessMode.Explicit)
      .addAclEntry(new FactAclEntryRecord().setSubjectID(currentUser));

    fact = withAcl(fact, currentUser, null);
    assertEquals(1, fact.getAcl().size());
    assertEquals(currentUser, fact.getAcl().get(0).getSubjectID());
  }
}
