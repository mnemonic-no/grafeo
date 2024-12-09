package no.mnemonic.services.grafeo.service.implementation.resolvers;

import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.services.grafeo.service.contexts.SecurityContext;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccessControlCriteriaResolverTest {

  private final Set<UUID> currentUserIdentities = SetUtils.set(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
  private final Set<UUID> availableOrganizationID = SetUtils.set(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

  @Mock
  private SecurityContext securityContext;
  @InjectMocks
  private AccessControlCriteriaResolver resolver;

  @BeforeEach
  public void setUp() {
    when(securityContext.getCurrentUserIdentities()).thenReturn(currentUserIdentities);
    when(securityContext.getAvailableOrganizationID()).thenReturn(availableOrganizationID);
  }

  @Test
  public void testGetResolvesAccessControlCriteria() throws Exception {
    AccessControlCriteria criteria = resolver.get();
    assertNotNull(criteria);
    assertEquals(currentUserIdentities, criteria.getCurrentUserIdentities());
    assertEquals(availableOrganizationID, criteria.getAvailableOrganizationID());

    verify(securityContext, times(availableOrganizationID.size()))
            .checkPermission(eq(FunctionConstants.viewGrafeoFact), argThat(availableOrganizationID::contains));
  }

  @Test
  public void testGetFiltersAvailableOrganizations() throws Exception {
    UUID available = UUID.randomUUID();
    UUID unavailable = UUID.randomUUID();
    when(securityContext.getAvailableOrganizationID()).thenReturn(SetUtils.set(available, unavailable));
    doNothing().when(securityContext).checkPermission(eq(FunctionConstants.viewGrafeoFact), eq(available)); // Without the test becomes unstable.
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(eq(FunctionConstants.viewGrafeoFact), eq(unavailable));

    AccessControlCriteria criteria = resolver.get();
    assertNotNull(criteria);
    assertEquals(currentUserIdentities, criteria.getCurrentUserIdentities());
    assertEquals(SetUtils.set(available), criteria.getAvailableOrganizationID());
  }
}
