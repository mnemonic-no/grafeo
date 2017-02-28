package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.FactComment;
import no.mnemonic.act.platform.api.request.v1.CreateFactCommentRequest;
import no.mnemonic.act.platform.entity.cassandra.FactCommentEntity;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;

import java.util.Objects;
import java.util.UUID;

public class FactCreateCommentDelegate extends AbstractDelegate {

  public static FactCreateCommentDelegate create() {
    return new FactCreateCommentDelegate();
  }

  public FactComment handle(CreateFactCommentRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch Fact and verify that it exists.
    FactEntity fact = fetchExistingFact(request.getFact());
    // Verify that user is allowed to access the Fact.
    TiSecurityContext.get().checkReadPermission(fact);
    // Verify that the 'replyTo' comment exists.
    verifyReplyToCommentExists(fact, request);
    // Save comment and return it to the user.
    return TiRequestContext.get().getFactCommentConverter().apply(saveComment(fact, request));
  }

  private void verifyReplyToCommentExists(FactEntity fact, CreateFactCommentRequest request) throws InvalidArgumentException {
    if (request.getReplyTo() == null) return;

    boolean exists = TiRequestContext.get().getFactManager()
            .fetchFactComments(fact.getId())
            .stream()
            .anyMatch(comment -> Objects.equals(comment.getId(), request.getReplyTo()));

    if (!exists) {
      throw new InvalidArgumentException()
              .addValidationError("Comment does not exist.", "comment.no.exists", "replyTo", request.getReplyTo().toString());
    }
  }

  private FactCommentEntity saveComment(FactEntity fact, CreateFactCommentRequest request) {
    FactCommentEntity comment = new FactCommentEntity()
            .setId(UUID.randomUUID()) // Need to provide client-generated ID.
            .setFactID(fact.getId())
            .setReplyToID(request.getReplyTo())
            .setSourceID(TiSecurityContext.get().getCurrentUserID())
            .setComment(request.getComment())
            .setTimestamp(System.currentTimeMillis());

    return TiRequestContext.get().getFactManager().saveFactComment(comment);
  }

}
