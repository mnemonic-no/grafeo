package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.v1.CreateFactCommentRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactCommentRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.converters.FactCommentConverter;
import no.mnemonic.act.platform.service.ti.resolvers.FactResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FactCreateCommentDelegateTest extends AbstractDelegateTest {

  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private FactResolver factResolver;
  @Mock
  private FactCommentConverter factCommentConverter;

  private FactCreateCommentDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    delegate = new FactCreateCommentDelegate(getSecurityContext(), objectFactDao, factResolver, factCommentConverter);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateFactCommentNoAccessToFact() throws Exception {
    CreateFactCommentRequest request = createFactCommentRequest();
    when(factResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(isA(FactRecord.class));

    delegate.handle(request);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateFactCommentNoAddPermission() throws Exception {
    CreateFactCommentRequest request = createFactCommentRequest();
    when(factResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(eq(TiFunctionConstants.addFactComments), any());

    delegate.handle(request);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testCreateFactCommentReplyToNotExists() throws Exception {
    CreateFactCommentRequest request = createFactCommentRequest();
    when(factResolver.resolveFact(request.getFact())).thenReturn(createFactRecord(request.getFact(), UUID.randomUUID()));

    delegate.handle(request);
  }

  @Test
  public void testCreateFactComment() throws Exception {
    UUID currentUser = UUID.randomUUID();
    CreateFactCommentRequest request = createFactCommentRequest();
    FactRecord fact = createFactRecord(request.getFact(), request.getReplyTo());
    when(factResolver.resolveFact(request.getFact())).thenReturn(fact);
    when(objectFactDao.storeFactComment(notNull(), notNull())).then(i -> i.getArgument(1));
    when(getSecurityContext().getCurrentUserID()).thenReturn(currentUser);

    delegate.handle(request);

    verify(objectFactDao).storeFactComment(same(fact), matchFactCommentRecord(request, currentUser));
    verify(factCommentConverter).apply(matchFactCommentRecord(request, currentUser));
  }

  private CreateFactCommentRequest createFactCommentRequest() {
    return new CreateFactCommentRequest()
            .setFact(UUID.randomUUID())
            .setReplyTo(UUID.randomUUID())
            .setComment("Hello World!");
  }

  private FactRecord createFactRecord(UUID id, UUID replyToID) {
    return new FactRecord()
            .setId(id)
            .addComment(new FactCommentRecord().setId(replyToID));
  }

  private FactCommentRecord matchFactCommentRecord(CreateFactCommentRequest request, UUID origin) {
    return argThat(comment -> {
      assertNotNull(comment.getId());
      assertEquals(request.getReplyTo(), comment.getReplyToID());
      assertEquals(origin, comment.getOriginID());
      assertEquals(request.getComment(), comment.getComment());
      assertTrue(comment.getTimestamp() > 0);
      return true;
    });
  }
}
