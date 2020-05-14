package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectTypeValueRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.ObjectTypeHandler;
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

public class TraverseByObjectTypeValueDelegateTest {

  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private TraverseGraphHandler traverseGraphHandler;
  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private ObjectTypeHandler objectTypeHandler;

  private TraverseByObjectTypeValueDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);

    delegate = new TraverseByObjectTypeValueDelegate(
            securityContext,
            traverseGraphHandler,
            objectFactDao,
            objectTypeHandler);
  }

  @Test
  public void testTraverseWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.traverseFactObjects);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new TraverseGraphByObjectTypeValueRequest()));
  }

  @Test
  public void testTraverseWithoutObject() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission((ObjectRecord) isNull());

    assertThrows(AccessDeniedException.class, () -> {
      delegate.handle(new TraverseGraphByObjectTypeValueRequest().setType("threatActor").setValue("Sofacy"));
    });

    verify(objectFactDao).getObject("threatActor", "Sofacy");
  }

  @Test
  public void testTraverseWithoutObjectType() throws Exception {
    TraverseGraphByObjectTypeValueRequest request = new TraverseGraphByObjectTypeValueRequest().setType("type").setValue("value");

    doThrow(InvalidArgumentException.class).when(objectTypeHandler).assertObjectTypeExists(request.getType(), "type");

    assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
  }

  @Test
  public void testTraverseWithObject() throws Exception {
    UUID objectId = UUID.randomUUID();
    String query = "g.outE()";

    when(objectFactDao.getObject("threatActor", "Sofacy")).thenReturn(new ObjectRecord().setId(objectId));

    delegate.handle(new TraverseGraphByObjectTypeValueRequest()
            .setType("threatActor")
            .setValue("Sofacy")
            .setQuery(query));

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
  public void testTraverseWithObjectAndParams() throws Exception {
    UUID objectId = UUID.randomUUID();
    String query = "g.outE()";

    when(objectFactDao.getObject("threatActor", "Sofacy")).thenReturn(new ObjectRecord().setId(objectId));

    Long before = 1L;
    Long after = 2L;
    delegate.handle(new TraverseGraphByObjectTypeValueRequest()
            .setType("threatActor")
            .setValue("Sofacy")
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
