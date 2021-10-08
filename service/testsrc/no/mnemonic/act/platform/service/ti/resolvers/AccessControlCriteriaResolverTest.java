package no.mnemonic.act.platform.service.ti.resolvers;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AccessControlCriteriaResolverTest {

  private final Set<UUID> currentUserIdentities = SetUtils.set(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
  private final Set<UUID> availableOrganizationID = SetUtils.set(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

  @Mock
  private SecurityContext securityContext;

  private AccessControlCriteriaResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);

    when(securityContext.getCurrentUserIdentities()).thenReturn(currentUserIdentities);
    when(securityContext.getAvailableOrganizationID()).thenReturn(availableOrganizationID);

    resolver = new AccessControlCriteriaResolver(securityContext);
  }

  @Test
  public void testGetResolvesAccessControlCriteria() throws Exception {
    AccessControlCriteria criteria = resolver.get();
    assertNotNull(criteria);
    assertEquals(currentUserIdentities, criteria.getCurrentUserIdentities());
    assertEquals(availableOrganizationID, criteria.getAvailableOrganizationID());

    verify(securityContext, times(availableOrganizationID.size()))
            .checkPermission(eq(TiFunctionConstants.viewThreatIntelFact), argThat(availableOrganizationID::contains));
  }

  @Test
  public void testGetFiltersAvailableOrganizations() throws Exception {
    UUID available = UUID.randomUUID();
    UUID unavailable = UUID.randomUUID();
    when(securityContext.getAvailableOrganizationID()).thenReturn(SetUtils.set(available, unavailable));
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(any(), eq(unavailable));

    AccessControlCriteria criteria = resolver.get();
    assertNotNull(criteria);
    assertEquals(currentUserIdentities, criteria.getCurrentUserIdentities());
    assertEquals(SetUtils.set(available), criteria.getAvailableOrganizationID());
  }
}
