package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.request.v1.CreateFactCommentRequest;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.FactCommentRecord;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.FactCommentResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactRequestResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FactCreateCommentDelegateTest {

  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private FactRequestResolver factRequestResolver;
  @Mock
  private FactCommentResponseConverter factCommentResponseConverter;
  @Mock
  private GrafeoSecurityContext securityContext;
  @InjectMocks
  private FactCreateCommentDelegate delegate;

  @Test
  public void testCreateFactCommentNoAccessToFact() throws Exception {
    CreateFactCommentRequest request = createFactCommentRequest();
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission(isA(FactRecord.class));

    assertThrows(AccessDeniedException.class, () -> delegate.handle(request));
  }

  @Test
  public void testCreateFactCommentNoAddPermission() throws Exception {
    CreateFactCommentRequest request = createFactCommentRequest();
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(eq(FunctionConstants.addGrafeoFactComment), any());

    assertThrows(AccessDeniedException.class, () -> delegate.handle(request));
  }

  @Test
  public void testCreateFactCommentReplyToNotExists() throws Exception {
    CreateFactCommentRequest request = createFactCommentRequest();
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(createFactRecord(request.getFact(), UUID.randomUUID()));

    assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
  }

  @Test
  public void testCreateFactComment() throws Exception {
    UUID currentUser = UUID.randomUUID();
    CreateFactCommentRequest request = createFactCommentRequest();
    FactRecord fact = createFactRecord(request.getFact(), request.getReplyTo());
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(fact);
    when(objectFactDao.storeFactComment(notNull(), notNull())).then(i -> i.getArgument(1));
    when(securityContext.getCurrentUserID()).thenReturn(currentUser);

    delegate.handle(request);

    verify(objectFactDao).storeFactComment(same(fact), matchFactCommentRecord(request, currentUser));
    verify(factCommentResponseConverter).apply(matchFactCommentRecord(request, currentUser));
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
