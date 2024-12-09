package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.model.v1.FactComment;
import no.mnemonic.services.grafeo.api.request.v1.GetFactCommentsRequest;
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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FactGetCommentsDelegateTest {

  @Mock
  private FactRequestResolver factRequestResolver;
  @Mock
  private FactCommentResponseConverter factCommentResponseConverter;
  @Mock
  private GrafeoSecurityContext securityContext;
  @InjectMocks
  private FactGetCommentsDelegate delegate;

  @Test
  public void testGetFactCommentsNoAccessToFact() throws Exception {
    GetFactCommentsRequest request = new GetFactCommentsRequest().setFact(UUID.randomUUID());
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission(isA(FactRecord.class));

    assertThrows(AccessDeniedException.class, () -> delegate.handle(request));
  }

  @Test
  public void testGetFactCommentsNoViewPermission() throws Exception {
    GetFactCommentsRequest request = new GetFactCommentsRequest().setFact(UUID.randomUUID());
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(eq(FunctionConstants.viewGrafeoFactComment), any());

    assertThrows(AccessDeniedException.class, () -> delegate.handle(request));
  }

  @Test
  public void testGetFactComments() throws Exception {
    GetFactCommentsRequest request = new GetFactCommentsRequest().setFact(UUID.randomUUID());
    List<FactCommentRecord> comments = createComments();
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord().setComments(comments));
    when(factCommentResponseConverter.apply(notNull())).thenReturn(FactComment.builder().build());

    ResultSet<FactComment> result = delegate.handle(request);

    assertEquals(comments.size(), result.getCount());
    assertEquals(0, result.getLimit());
    assertEquals(comments.size(), ListUtils.list(result.iterator()).size());
    verify(factCommentResponseConverter, times(comments.size())).apply(argThat(comments::contains));
  }

  @Test
  public void testGetFactCommentsFilterByBefore() throws Exception {
    GetFactCommentsRequest request = new GetFactCommentsRequest().setFact(UUID.randomUUID()).setBefore(150L);
    List<FactCommentRecord> comments = createComments();
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord().setComments(comments));

    assertEquals(1, delegate.handle(request).getCount());
    verify(factCommentResponseConverter).apply(comments.get(0));
    verifyNoMoreInteractions(factCommentResponseConverter);
  }

  @Test
  public void testGetFactCommentsFilterByAfter() throws Exception {
    GetFactCommentsRequest request = new GetFactCommentsRequest().setFact(UUID.randomUUID()).setAfter(250L);
    List<FactCommentRecord> comments = createComments();
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord().setComments(comments));

    assertEquals(1, delegate.handle(request).getCount());
    verify(factCommentResponseConverter).apply(comments.get(2));
    verifyNoMoreInteractions(factCommentResponseConverter);
  }

  private FactCommentRecord createComment(long timestamp) {
    return new FactCommentRecord()
            .setId(UUID.randomUUID())
            .setReplyToID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setComment("Hello World!")
            .setTimestamp(timestamp);
  }

  private List<FactCommentRecord> createComments() {
    return ListUtils.list(createComment(100), createComment(200), createComment(300));
  }
}
