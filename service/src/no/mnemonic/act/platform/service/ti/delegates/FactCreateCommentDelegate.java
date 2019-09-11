package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.FactComment;
import no.mnemonic.act.platform.api.request.v1.CreateFactCommentRequest;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactCommentEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;

import javax.inject.Inject;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class FactCreateCommentDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final FactManager factManager;
  private final Function<FactCommentEntity, FactComment> factCommentConverter;

  @Inject
  public FactCreateCommentDelegate(TiSecurityContext securityContext,
                                   FactManager factManager,
                                   Function<FactCommentEntity, FactComment> factCommentConverter) {
    this.securityContext = securityContext;
    this.factManager = factManager;
    this.factCommentConverter = factCommentConverter;
  }

  public FactComment handle(CreateFactCommentRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch Fact and verify that it exists.
    FactEntity fact = fetchExistingFact(request.getFact());
    // Verify that user is allowed to access the Fact.
    securityContext.checkReadPermission(fact);
    // Verify that user is allowed to comment on the Fact.
    securityContext.checkPermission(TiFunctionConstants.addFactComments, fact.getOrganizationID());
    // Verify that the 'replyTo' comment exists.
    verifyReplyToCommentExists(fact, request);
    // Save comment and return it to the user.
    return factCommentConverter.apply(saveComment(fact, request));
  }

  private void verifyReplyToCommentExists(FactEntity fact, CreateFactCommentRequest request) throws InvalidArgumentException {
    if (request.getReplyTo() == null) return;

    boolean exists = factManager
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
            .setOriginID(securityContext.getCurrentUserID())
            .setComment(request.getComment())
            .setTimestamp(System.currentTimeMillis());

    return factManager.saveFactComment(comment);
  }
}
