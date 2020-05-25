package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectsRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.ObjectTypeHandler;
import no.mnemonic.act.platform.service.ti.handlers.TraverseGraphHandler;
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
  private TiSecurityContext securityContext;
  @Mock
  private TraverseGraphHandler traverseGraphHandler;
  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private ObjectTypeHandler objectTypeHandler;

  private TraverseByObjectsDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);

    delegate = new TraverseByObjectsDelegate(
            securityContext,
            traverseGraphHandler,
            objectFactDao,
            objectTypeHandler
    );
  }

  @Test
  public void testTraverseWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.traverseFactObjects);
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

    verify(traverseGraphHandler).traverse(
            eq(set(objectId1, objectId2)),
            eq(query),
            argThat(traverseParams -> {
              assertFalse(traverseParams.isIncludeRetracted());
              assertNull(traverseParams.getAfterTimestamp());
              assertNull(traverseParams.getBeforeTimestamp());
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

    Long before = 1L;
    Long after = 2L;
    TraverseGraphByObjectsRequest request = new TraverseGraphByObjectsRequest()
            .setQuery(query)
            .setBefore(before)
            .setAfter(after)
            .setIncludeRetracted(true)
            .setLimit(10)
            .setObjects(set(objectId1.toString(), objectId2.toString()));

    delegate.handle(request);

    verify(traverseGraphHandler).traverse(
            eq(set(objectId1, objectId2)),
            eq(query),
            argThat(traverseParams -> {
              assertTrue(traverseParams.isIncludeRetracted());
              assertEquals(after, traverseParams.getAfterTimestamp());
              assertEquals(before, traverseParams.getBeforeTimestamp());
              assertEquals(10, traverseParams.getLimit());
              return true;
            }));
  }
}
