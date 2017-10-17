package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.CreateFactCommentRequest;
import no.mnemonic.act.platform.entity.cassandra.FactCommentEntity;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FactCreateCommentDelegateTest extends AbstractDelegateTest {

  @Test(expected = ObjectNotFoundException.class)
  public void testCreateFactCommentFactNotExists() throws Exception {
    FactCreateCommentDelegate.create().handle(createFactCommentRequest());
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateFactCommentNoAccessToFact() throws Exception {
    CreateFactCommentRequest request = createFactCommentRequest();
    when(getFactManager().getFact(request.getFact())).thenReturn(new FactEntity());
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkReadPermission(any());

    FactCreateCommentDelegate.create().handle(request);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testCreateFactCommentReplyToNotExists() throws Exception {
    CreateFactCommentRequest request = createFactCommentRequest();
    when(getFactManager().getFact(request.getFact())).thenReturn(createFactEntity(request.getFact()));
    when(getFactManager().fetchFactComments(request.getFact())).thenReturn(createExistingComments(UUID.randomUUID()));

    FactCreateCommentDelegate.create().handle(request);
  }

  @Test
  public void testCreateFactComment() throws Exception {
    UUID currentUser = UUID.randomUUID();
    CreateFactCommentRequest request = createFactCommentRequest();
    when(getFactManager().getFact(request.getFact())).thenReturn(createFactEntity(request.getFact()));
    when(getFactManager().fetchFactComments(request.getFact())).thenReturn(createExistingComments(request.getReplyTo()));
    when(getFactManager().saveFactComment(any())).then(i -> i.getArgument(0));
    when(getSecurityContext().getCurrentUserID()).thenReturn(currentUser);

    FactCreateCommentDelegate.create().handle(request);

    verify(getFactManager()).saveFactComment(matchFactCommentEntity(request, currentUser));
    verify(getFactCommentConverter()).apply(matchFactCommentEntity(request, currentUser));
  }

  private CreateFactCommentRequest createFactCommentRequest() {
    return new CreateFactCommentRequest()
            .setFact(UUID.randomUUID())
            .setReplyTo(UUID.randomUUID())
            .setComment("Hello World!");
  }

  private FactEntity createFactEntity(UUID id) {
    return new FactEntity().setId(id);
  }

  private List<FactCommentEntity> createExistingComments(UUID replyToID) {
    return ListUtils.list(new FactCommentEntity().setId(replyToID));
  }

  private FactCommentEntity matchFactCommentEntity(CreateFactCommentRequest request, UUID source) {
    return argThat(comment -> {
      assertNotNull(comment.getId());
      assertEquals(request.getFact(), comment.getFactID());
      assertEquals(request.getReplyTo(), comment.getReplyToID());
      assertEquals(source, comment.getSourceID());
      assertEquals(request.getComment(), comment.getComment());
      assertTrue(comment.getTimestamp() > 0);
      return true;
    });
  }

}
