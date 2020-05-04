package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectIdRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.TraverseGraphHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class TraverseByObjectIdDelegateTest {

  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private TraverseGraphHandler traverseGraphHandler;
  @Mock
  private ObjectFactDao objectFactDao;

  private TraverseByObjectIdDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);

    delegate = new TraverseByObjectIdDelegate(
            securityContext,
            traverseGraphHandler,
            objectFactDao
    );
  }

  @Test
  public void testTraverseGraphByObjectIdWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.traverseFactObjects);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new TraverseGraphByObjectIdRequest()));
  }

  @Test
  public void testGraphWithoutObject() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission((ObjectRecord) isNull());

    UUID objectId = UUID.randomUUID();

    assertThrows(AccessDeniedException.class, () -> delegate.handle(new TraverseGraphByObjectIdRequest().setId(objectId)));

    verify(objectFactDao).getObject(objectId);
  }

  @Test
  public void testGraphWithObject() throws Exception {
    UUID objectId = UUID.randomUUID();
    String query = "g.outE()";

    when(objectFactDao.getObject(objectId)).thenReturn(new ObjectRecord().setId(objectId));

    delegate.handle(new TraverseGraphByObjectIdRequest().setId(objectId).setQuery(query));

    verify(traverseGraphHandler).traverse(
            eq(Collections.singleton(objectId)),
            eq(query),
            argThat(traverseParams -> {
              assertFalse(traverseParams.isIncludeRetracted());
              assertNull(traverseParams.getAfterTimestamp());
              assertNull(traverseParams.getBeforeTimestamp());
              return true;
            }));
  }

  @Test
  public void testGraphWithObjectAndParams() throws Exception {
    UUID objectId = UUID.randomUUID();
    String query = "g.outE()";

    when(objectFactDao.getObject(objectId)).thenReturn(new ObjectRecord().setId(objectId));

    Long before = 1L;
    Long after = 2L;
    delegate.handle(new TraverseGraphByObjectIdRequest()
            .setId(objectId)
            .setQuery(query)
            .setIncludeRetracted(true)
            .setBefore(before)
            .setAfter(after));

    verify(traverseGraphHandler).traverse(
            eq(Collections.singleton(objectId)),
            eq(query),
            argThat(traverseParams -> {
              assertTrue(traverseParams.isIncludeRetracted());
              assertEquals(traverseParams.getAfterTimestamp(), after);
              assertEquals(traverseParams.getBeforeTimestamp(), before);
              return true;
            }));
  }
}
