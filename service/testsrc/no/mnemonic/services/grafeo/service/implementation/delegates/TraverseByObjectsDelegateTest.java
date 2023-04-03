package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.request.v1.TraverseGraphByObjectsRequest;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.handlers.ObjectTypeHandler;
import no.mnemonic.services.grafeo.service.implementation.handlers.TraverseGraphHandler;
import no.mnemonic.services.grafeo.service.implementation.resolvers.AccessControlCriteriaResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.IndexSelectCriteriaResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class TraverseByObjectsDelegateTest {

  @Mock
  private GrafeoSecurityContext securityContext;
  @Mock
  private TraverseGraphHandler traverseGraphHandler;
  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private ObjectTypeHandler objectTypeHandler;
  @Mock
  private AccessControlCriteriaResolver accessControlCriteriaResolver;
  @Mock
  private IndexSelectCriteriaResolver indexSelectCriteriaResolver;

  private TraverseByObjectsDelegate delegate;

  @Before
  public void setup() throws Exception {
    initMocks(this);

    when(accessControlCriteriaResolver.get()).thenReturn(AccessControlCriteria.builder()
            .addCurrentUserIdentity(UUID.randomUUID())
            .addAvailableOrganizationID(UUID.randomUUID())
            .build());
    when(indexSelectCriteriaResolver.validateAndCreateCriteria(any(), any()))
            .thenReturn(IndexSelectCriteria.builder().build());

    delegate = new TraverseByObjectsDelegate(
            securityContext,
            traverseGraphHandler,
            objectFactDao,
            objectTypeHandler,
            accessControlCriteriaResolver,
            indexSelectCriteriaResolver);
  }

  @Test
  public void testTraverseWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.traverseThreatIntelFact);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new TraverseGraphByObjectsRequest()));
  }

  @Test
  public void testTraverseWithoutObject() throws Exception {

    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission((ObjectRecord) isNull());

    assertThrows(AccessDeniedException.class, () -> {
      delegate.handle(new TraverseGraphByObjectsRequest().setObjects(set("ThreatActor/Sofacy")));
    });

    verify(objectFactDao).getObject("ThreatActor", "Sofacy");
  }


  @Test
  public void testTraverseWithoutObjectType() throws Exception {
    TraverseGraphByObjectsRequest request = new TraverseGraphByObjectsRequest()
            .setObjects(set("ThreatActor/Sofacy"));

    doThrow(InvalidArgumentException.class).when(objectTypeHandler).assertObjectTypeExists("ThreatActor", "type");

    assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
  }

  @Test
  public void testTraverseWithObjects() throws Exception {
    UUID objectId1 = UUID.randomUUID();
    UUID objectId2 = UUID.randomUUID();
    String query = "g.outE()";

    when(objectFactDao.getObject("ThreatActor", "Sofacy")).thenReturn(new ObjectRecord().setId(objectId1));
    when(objectFactDao.getObject(objectId2)).thenReturn(new ObjectRecord().setId(objectId2));

    TraverseGraphByObjectsRequest request = new TraverseGraphByObjectsRequest()
            .setQuery(query)
            .setObjects(set(
                    "ThreatActor/Sofacy",
                    objectId2.toString()));

    delegate.handle(request);

    verify(indexSelectCriteriaResolver).validateAndCreateCriteria(isNull(), isNull());
    verify(traverseGraphHandler).traverse(
            eq(set(objectId1, objectId2)),
            eq(query),
            argThat(traverseParams -> {
              assertNull(traverseParams.getBaseSearchCriteria().getStartTimestamp());
              assertNull(traverseParams.getBaseSearchCriteria().getEndTimestamp());
              assertNotNull(traverseParams.getBaseSearchCriteria().getAccessControlCriteria());
              assertNotNull(traverseParams.getBaseSearchCriteria().getIndexSelectCriteria());
              assertFalse(traverseParams.isIncludeRetracted());
              assertEquals(25, traverseParams.getLimit());
              return true;
            }));
  }

  @Test
  public void testTraverseWithObjectsAndParams() throws Exception {
    UUID objectId1 = UUID.randomUUID();
    UUID objectId2 = UUID.randomUUID();
    String query = "g.outE()";

    when(objectFactDao.getObject(objectId1)).thenReturn(new ObjectRecord().setId(objectId1));
    when(objectFactDao.getObject(objectId2)).thenReturn(new ObjectRecord().setId(objectId2));

    Long startTimestamp = 1L;
    Long endTimestamp = 2L;
    TraverseGraphByObjectsRequest request = new TraverseGraphByObjectsRequest()
            .setQuery(query)
            .setStartTimestamp(startTimestamp)
            .setEndTimestamp(endTimestamp)
            .setIncludeRetracted(true)
            .setLimit(10)
            .setObjects(set(objectId1.toString(), objectId2.toString()));

    delegate.handle(request);

    verify(indexSelectCriteriaResolver).validateAndCreateCriteria(startTimestamp, endTimestamp);
    verify(traverseGraphHandler).traverse(
            eq(set(objectId1, objectId2)),
            eq(query),
            argThat(traverseParams -> {
              assertEquals(startTimestamp, traverseParams.getBaseSearchCriteria().getStartTimestamp());
              assertEquals(endTimestamp, traverseParams.getBaseSearchCriteria().getEndTimestamp());
              assertNotNull(traverseParams.getBaseSearchCriteria().getAccessControlCriteria());
              assertNotNull(traverseParams.getBaseSearchCriteria().getIndexSelectCriteria());
              assertTrue(traverseParams.isIncludeRetracted());
              assertEquals(10, traverseParams.getLimit());
              return true;
            }));
  }
}
