package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.GetFactByIdRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

public class FactGetByIdDelegateTest extends AbstractDelegateTest {

  @Test(expected = AccessDeniedException.class)
  public void testFetchFactWithoutViewPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.viewFactObjects);
    FactGetByIdDelegate.create().handle(new GetFactByIdRequest());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testFetchFactNotFound() throws Exception {
    UUID id = UUID.randomUUID();
    FactGetByIdDelegate.create().handle(new GetFactByIdRequest().setId(id));
    verify(getFactManager()).getFact(id);
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchFactNoAccess() throws Exception {
    UUID id = UUID.randomUUID();
    FactEntity entity = new FactEntity();

    when(getFactManager().getFact(id)).thenReturn(entity);
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(entity);

    FactGetByIdDelegate.create().handle(new GetFactByIdRequest().setId(id));
  }

  @Test
  public void testFetchFact() throws Exception {
    UUID id = UUID.randomUUID();
    FactEntity entity = new FactEntity();

    when(getFactManager().getFact(id)).thenReturn(entity);
    FactGetByIdDelegate.create().handle(new GetFactByIdRequest().setId(id));
    verify(getFactConverter()).apply(entity);
  }

  @Test
  public void testFetchFactWithInReferenceTo() throws Exception {
    FactEntity inReferenceTo = new FactEntity().setId(UUID.randomUUID());
    FactEntity fact = new FactEntity().setId(UUID.randomUUID()).setInReferenceToID(inReferenceTo.getId());

    when(getFactManager().getFact(fact.getId())).thenReturn(fact);
    when(getFactManager().getFact(inReferenceTo.getId())).thenReturn(inReferenceTo);
    FactGetByIdDelegate.create().handle(new GetFactByIdRequest().setId(fact.getId()));
    verify(getFactConverter()).apply(argThat(entity -> entity == fact && entity.getInReferenceToID().equals(inReferenceTo.getId())));
  }

  @Test
  public void testFetchFactNoAccessToInReferenceTo() throws Exception {
    FactEntity inReferenceTo = new FactEntity().setId(UUID.randomUUID());
    FactEntity fact = new FactEntity().setId(UUID.randomUUID()).setInReferenceToID(inReferenceTo.getId());

    when(getFactManager().getFact(fact.getId())).thenReturn(fact);
    when(getFactManager().getFact(inReferenceTo.getId())).thenReturn(inReferenceTo);
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(inReferenceTo);
    FactGetByIdDelegate.create().handle(new GetFactByIdRequest().setId(fact.getId()));
    verify(getFactConverter()).apply(argThat(entity -> entity.getId().equals(fact.getId()) && entity.getInReferenceToID() == null));
  }

}
