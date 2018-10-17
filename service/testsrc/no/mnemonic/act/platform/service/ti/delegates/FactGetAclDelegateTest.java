package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.AclEntry;
import no.mnemonic.act.platform.api.request.v1.GetFactAclRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.dao.cassandra.entity.FactAclEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class FactGetAclDelegateTest extends AbstractDelegateTest {

  @Test(expected = ObjectNotFoundException.class)
  public void testGetFactAclFactNotExists() throws Exception {
    FactGetAclDelegate.create().handle(new GetFactAclRequest());
  }

  @Test(expected = AccessDeniedException.class)
  public void testGetFactAclNoAccessToFact() throws Exception {
    GetFactAclRequest request = new GetFactAclRequest().setFact(UUID.randomUUID());
    when(getFactManager().getFact(request.getFact())).thenReturn(new FactEntity());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(isA(FactEntity.class));

    FactGetAclDelegate.create().handle(request);
  }

  @Test(expected = AccessDeniedException.class)
  public void testGetFactAclNoViewPermission() throws Exception {
    GetFactAclRequest request = new GetFactAclRequest().setFact(UUID.randomUUID());
    when(getFactManager().getFact(request.getFact())).thenReturn(new FactEntity());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(eq(TiFunctionConstants.viewFactAccess), any());

    FactGetAclDelegate.create().handle(request);
  }

  @Test
  public void testGetFactAcl() throws Exception {
    GetFactAclRequest request = new GetFactAclRequest().setFact(UUID.randomUUID());
    List<FactAclEntity> entities = ListUtils.list(new FactAclEntity(), new FactAclEntity(), new FactAclEntity());
    when(getFactManager().getFact(request.getFact())).thenReturn(new FactEntity().setId(request.getFact()));
    when(getFactManager().fetchFactAcl(request.getFact())).thenReturn(entities);

    ResultSet<AclEntry> result = FactGetAclDelegate.create().handle(request);

    assertEquals(entities.size(), result.getCount());
    assertEquals(0, result.getLimit());
    assertEquals(entities.size(), result.getValues().size());
    verify(getAclEntryConverter(), times(entities.size())).apply(argThat(entities::contains));
  }

}
