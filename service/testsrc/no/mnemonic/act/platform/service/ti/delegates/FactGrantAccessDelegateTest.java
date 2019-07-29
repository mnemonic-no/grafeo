package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.GrantFactAccessRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.AccessMode;
import no.mnemonic.act.platform.dao.cassandra.entity.FactAclEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class FactGrantAccessDelegateTest extends AbstractDelegateTest {

  private FactGrantAccessDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new FactGrantAccessDelegate(getSecurityContext(), getFactManager(), getAclEntryConverter());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testGrantFactAccessFactNotExists() throws Exception {
    delegate.handle(createGrantAccessRequest());
  }

  @Test(expected = AccessDeniedException.class)
  public void testGrantFactAccessNoAccessToFact() throws Exception {
    GrantFactAccessRequest request = createGrantAccessRequest();
    when(getFactManager().getFact(request.getFact())).thenReturn(new FactEntity());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(isA(FactEntity.class));

    delegate.handle(request);
  }

  @Test(expected = AccessDeniedException.class)
  public void testGrantFactAccessNoGrantPermission() throws Exception {
    GrantFactAccessRequest request = createGrantAccessRequest();
    when(getFactManager().getFact(request.getFact())).thenReturn(new FactEntity());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(eq(TiFunctionConstants.grantFactAccess), any());

    delegate.handle(request);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testGrantFactAccessToPublicFact() throws Exception {
    GrantFactAccessRequest request = createGrantAccessRequest();
    when(getFactManager().getFact(request.getFact())).thenReturn(new FactEntity().setAccessMode(AccessMode.Public));

    delegate.handle(request);
  }

  @Test
  public void testGrantFactAccessSubjectAlreadyInAcl() throws Exception {
    GrantFactAccessRequest request = createGrantAccessRequest();
    FactAclEntity existingEntry = createFactAclEntity(request);
    when(getFactManager().getFact(request.getFact())).thenReturn(createFactEntity(request));
    when(getFactManager().fetchFactAcl(request.getFact())).thenReturn(ListUtils.list(existingEntry));

    delegate.handle(request);

    verify(getFactManager(), never()).saveFactAclEntry(any());
    verify(getFactSearchManager(), never()).indexFact(any());
    verify(getAclEntryConverter()).apply(matchFactAclEntity(request, existingEntry.getSourceID()));
  }

  @Test
  public void testGrantFactAccess() throws Exception {
    UUID currentUser = UUID.randomUUID();
    GrantFactAccessRequest request = createGrantAccessRequest();
    when(getFactManager().getFact(request.getFact())).thenReturn(createFactEntity(request));
    when(getFactSearchManager().getFact(request.getFact())).thenReturn(new FactDocument());
    when(getFactManager().saveFactAclEntry(any())).then(i -> i.getArgument(0));
    when(getSecurityContext().getCurrentUserID()).thenReturn(currentUser);

    delegate.handle(request);

    verify(getFactManager()).saveFactAclEntry(matchFactAclEntity(request, currentUser));
    verify(getFactSearchManager()).indexFact(argThat(document -> document.getAcl().contains(request.getSubject())));
    verify(getAclEntryConverter()).apply(matchFactAclEntity(request, currentUser));
  }

  private GrantFactAccessRequest createGrantAccessRequest() {
    return new GrantFactAccessRequest()
            .setFact(UUID.randomUUID())
            .setSubject(UUID.randomUUID());
  }

  private FactEntity createFactEntity(GrantFactAccessRequest request) {
    return new FactEntity()
            .setId(request.getFact())
            .setAccessMode(AccessMode.RoleBased);
  }

  private FactAclEntity createFactAclEntity(GrantFactAccessRequest request) {
    return new FactAclEntity()
            .setFactID(request.getFact())
            .setSubjectID(request.getSubject())
            .setId(UUID.randomUUID())
            .setSourceID(UUID.randomUUID())
            .setTimestamp(123456789);
  }

  private FactAclEntity matchFactAclEntity(GrantFactAccessRequest request, UUID source) {
    return argThat(entry -> {
      assertNotNull(entry.getId());
      assertEquals(request.getFact(), entry.getFactID());
      assertEquals(request.getSubject(), entry.getSubjectID());
      assertEquals(source, entry.getSourceID());
      assertTrue(entry.getTimestamp() > 0);
      return true;
    });
  }
}
