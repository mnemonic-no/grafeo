package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.v1.GrantFactAccessRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.AclEntryResponseConverter;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactRequestResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactGrantAccessDelegateTest {

  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private FactRequestResolver factRequestResolver;
  @Mock
  private AclEntryResponseConverter aclEntryResponseConverter;
  @Mock
  private TiSecurityContext securityContext;

  private FactGrantAccessDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);
    delegate = new FactGrantAccessDelegate(securityContext, objectFactDao, factRequestResolver, aclEntryResponseConverter);
  }

  @Test(expected = AccessDeniedException.class)
  public void testGrantFactAccessNoAccessToFact() throws Exception {
    GrantFactAccessRequest request = createGrantAccessRequest();
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission(isA(FactRecord.class));

    delegate.handle(request);
  }

  @Test(expected = AccessDeniedException.class)
  public void testGrantFactAccessNoGrantPermission() throws Exception {
    GrantFactAccessRequest request = createGrantAccessRequest();
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(eq(TiFunctionConstants.grantFactAccess), any());

    delegate.handle(request);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testGrantFactAccessToPublicFact() throws Exception {
    GrantFactAccessRequest request = createGrantAccessRequest();
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord().setAccessMode(FactRecord.AccessMode.Public));

    delegate.handle(request);
  }

  @Test
  public void testGrantFactAccessSubjectAlreadyInAcl() throws Exception {
    GrantFactAccessRequest request = createGrantAccessRequest();
    FactAclEntryRecord existingEntry = createFactAclEntryRecord(request);
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(createFactRecord(request).addAclEntry(existingEntry));

    delegate.handle(request);

    verify(objectFactDao, never()).storeFactAclEntry(any(), any());
    verify(aclEntryResponseConverter).apply(matchFactAclEntryRecord(request, existingEntry.getOriginID()));
  }

  @Test
  public void testGrantFactAccess() throws Exception {
    UUID currentUser = UUID.randomUUID();
    GrantFactAccessRequest request = createGrantAccessRequest();
    FactRecord fact = createFactRecord(request);
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(fact);
    when(objectFactDao.storeFactAclEntry(notNull(), notNull())).then(i -> i.getArgument(1));
    when(securityContext.getCurrentUserID()).thenReturn(currentUser);

    delegate.handle(request);

    verify(objectFactDao).storeFactAclEntry(same(fact), matchFactAclEntryRecord(request, currentUser));
    verify(aclEntryResponseConverter).apply(matchFactAclEntryRecord(request, currentUser));
  }

  private GrantFactAccessRequest createGrantAccessRequest() {
    return new GrantFactAccessRequest()
            .setFact(UUID.randomUUID())
            .setSubject(UUID.randomUUID());
  }

  private FactRecord createFactRecord(GrantFactAccessRequest request) {
    return new FactRecord()
            .setId(request.getFact())
            .setAccessMode(FactRecord.AccessMode.RoleBased);
  }

  private FactAclEntryRecord createFactAclEntryRecord(GrantFactAccessRequest request) {
    return new FactAclEntryRecord()
            .setSubjectID(request.getSubject())
            .setId(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setTimestamp(123456789);
  }

  private FactAclEntryRecord matchFactAclEntryRecord(GrantFactAccessRequest request, UUID origin) {
    return argThat(entry -> {
      assertNotNull(entry.getId());
      assertEquals(request.getSubject(), entry.getSubjectID());
      assertEquals(origin, entry.getOriginID());
      assertTrue(entry.getTimestamp() > 0);
      return true;
    });
  }
}
