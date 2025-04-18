package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.model.v1.AclEntry;
import no.mnemonic.services.grafeo.api.request.v1.GetFactAclRequest;
import no.mnemonic.services.grafeo.dao.api.record.FactAclEntryRecord;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.AclEntryResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactRequestResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FactGetAclDelegateTest {

  @Mock
  private FactRequestResolver factRequestResolver;
  @Mock
  private AclEntryResponseConverter aclEntryResponseConverter;
  @Mock
  private GrafeoSecurityContext securityContext;
  @InjectMocks
  private FactGetAclDelegate delegate;

  @Test
  public void testGetFactAclNoAccessToFact() throws Exception {
    GetFactAclRequest request = new GetFactAclRequest().setFact(UUID.randomUUID());
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission(isA(FactRecord.class));

    assertThrows(AccessDeniedException.class, () -> delegate.handle(request));
  }

  @Test
  public void testGetFactAclNoViewPermission() throws Exception {
    GetFactAclRequest request = new GetFactAclRequest().setFact(UUID.randomUUID());
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(eq(FunctionConstants.viewGrafeoFactAccess), any());

    assertThrows(AccessDeniedException.class, () -> delegate.handle(request));
  }

  @Test
  public void testGetFactAcl() throws Exception {
    GetFactAclRequest request = new GetFactAclRequest().setFact(UUID.randomUUID());
    List<FactAclEntryRecord> acl = ListUtils.list(new FactAclEntryRecord(), new FactAclEntryRecord(), new FactAclEntryRecord());
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord().setAcl(acl));
    when(aclEntryResponseConverter.apply(notNull())).thenReturn(AclEntry.builder().build());

    ResultSet<AclEntry> result = delegate.handle(request);

    assertEquals(acl.size(), result.getCount());
    assertEquals(0, result.getLimit());
    assertEquals(acl.size(), ListUtils.list(result.iterator()).size());
    verify(aclEntryResponseConverter, times(acl.size())).apply(argThat(acl::contains));
  }
}
