package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.FactComment;
import no.mnemonic.services.grafeo.api.request.v1.CreateFactCommentRequest;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.FactCommentRecord;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.FactCommentResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactRequestResolver;

import javax.inject.Inject;
import java.util.Objects;
import java.util.UUID;

public class FactCreateCommentDelegate implements Delegate {

  private final GrafeoSecurityContext securityContext;
  private final ObjectFactDao objectFactDao;
  private final FactRequestResolver factRequestResolver;
  private final FactCommentResponseConverter factCommentResponseConverter;

  @Inject
  public FactCreateCommentDelegate(GrafeoSecurityContext securityContext,
                                   ObjectFactDao objectFactDao,
                                   FactRequestResolver factRequestResolver,
                                   FactCommentResponseConverter factCommentResponseConverter) {
    this.securityContext = securityContext;
    this.objectFactDao = objectFactDao;
    this.factRequestResolver = factRequestResolver;
    this.factCommentResponseConverter = factCommentResponseConverter;
  }

  public FactComment handle(CreateFactCommentRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch Fact and verify that it exists.
    FactRecord fact = factRequestResolver.resolveFact(request.getFact());
    // Verify that user is allowed to access the Fact.
    securityContext.checkReadPermission(fact);
    // Verify that user is allowed to comment on the Fact.
    securityContext.checkPermission(FunctionConstants.addGrafeoFactComment, fact.getOrganizationID());
    // Verify that the 'replyTo' comment exists.
    verifyReplyToCommentExists(fact, request);
    // Save comment and return it to the user.
    return factCommentResponseConverter.apply(saveComment(fact, request));
  }

  private void verifyReplyToCommentExists(FactRecord fact, CreateFactCommentRequest request) throws InvalidArgumentException {
    if (request.getReplyTo() == null) return;

    boolean exists = ListUtils.list(fact.getComments())
            .stream()
            .anyMatch(comment -> Objects.equals(comment.getId(), request.getReplyTo()));

    if (!exists) {
      throw new InvalidArgumentException()
              .addValidationError("Comment does not exist.", "comment.no.exists", "replyTo", request.getReplyTo().toString());
    }
  }

  private FactCommentRecord saveComment(FactRecord fact, CreateFactCommentRequest request) {
    FactCommentRecord comment = new FactCommentRecord()
            .setId(UUID.randomUUID())
            .setReplyToID(request.getReplyTo())
            .setOriginID(securityContext.getCurrentUserID())
            .setComment(request.getComment())
            .setTimestamp(System.currentTimeMillis());

    return objectFactDao.storeFactComment(fact, comment);
  }
}
