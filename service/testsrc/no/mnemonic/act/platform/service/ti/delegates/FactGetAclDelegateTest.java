package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.AclEntry;
import no.mnemonic.act.platform.api.request.v1.GetFactAclRequest;
import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.AclEntryResponseConverter;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactRequestResolver;
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
import static org.mockito.MockitoAnnotations.initMocks;

public class FactGetAclDelegateTest {

  @Mock
  private FactRequestResolver factRequestResolver;
  @Mock
  private AclEntryResponseConverter aclEntryResponseConverter;
  @Mock
  private TiSecurityContext securityContext;

  private FactGetAclDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);
    delegate = new FactGetAclDelegate(securityContext, factRequestResolver, aclEntryResponseConverter);
  }

  @Test(expected = AccessDeniedException.class)
  public void testGetFactAclNoAccessToFact() throws Exception {
    GetFactAclRequest request = new GetFactAclRequest().setFact(UUID.randomUUID());
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission(isA(FactRecord.class));

    delegate.handle(request);
  }

  @Test(expected = AccessDeniedException.class)
  public void testGetFactAclNoViewPermission() throws Exception {
    GetFactAclRequest request = new GetFactAclRequest().setFact(UUID.randomUUID());
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(eq(TiFunctionConstants.viewFactAccess), any());

    delegate.handle(request);
  }

  @Test
  public void testGetFactAcl() throws Exception {
    GetFactAclRequest request = new GetFactAclRequest().setFact(UUID.randomUUID());
    List<FactAclEntryRecord> acl = ListUtils.list(new FactAclEntryRecord(), new FactAclEntryRecord(), new FactAclEntryRecord());
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord().setAcl(acl));

    ResultSet<AclEntry> result = delegate.handle(request);

    assertEquals(acl.size(), result.getCount());
    assertEquals(0, result.getLimit());
    assertEquals(acl.size(), ListUtils.list(result.iterator()).size());
    verify(aclEntryResponseConverter, times(acl.size())).apply(argThat(acl::contains));
  }
}
