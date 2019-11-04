package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.AclEntry;
import no.mnemonic.act.platform.api.request.v1.GetFactAclRequest;
import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.converters.AclEntryConverter;
import no.mnemonic.act.platform.service.ti.resolvers.FactResolver;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class FactGetAclDelegateTest extends AbstractDelegateTest {

  @Mock
  private FactResolver factResolver;
  @Mock
  private AclEntryConverter aclEntryConverter;

  private FactGetAclDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new FactGetAclDelegate(getSecurityContext(), factResolver, aclEntryConverter);
  }

  @Test(expected = AccessDeniedException.class)
  public void testGetFactAclNoAccessToFact() throws Exception {
    GetFactAclRequest request = new GetFactAclRequest().setFact(UUID.randomUUID());
    when(factResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(isA(FactRecord.class));

    delegate.handle(request);
  }

  @Test(expected = AccessDeniedException.class)
  public void testGetFactAclNoViewPermission() throws Exception {
    GetFactAclRequest request = new GetFactAclRequest().setFact(UUID.randomUUID());
    when(factResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(eq(TiFunctionConstants.viewFactAccess), any());

    delegate.handle(request);
  }

  @Test
  public void testGetFactAcl() throws Exception {
    GetFactAclRequest request = new GetFactAclRequest().setFact(UUID.randomUUID());
    List<FactAclEntryRecord> acl = ListUtils.list(new FactAclEntryRecord(), new FactAclEntryRecord(), new FactAclEntryRecord());
    when(factResolver.resolveFact(request.getFact())).thenReturn(new FactRecord().setAcl(acl));

    ResultSet<AclEntry> result = delegate.handle(request);

    assertEquals(acl.size(), result.getCount());
    assertEquals(0, result.getLimit());
    assertEquals(acl.size(), ListUtils.list(result.iterator()).size());
    verify(aclEntryConverter, times(acl.size())).apply(argThat(acl::contains));
  }
}
