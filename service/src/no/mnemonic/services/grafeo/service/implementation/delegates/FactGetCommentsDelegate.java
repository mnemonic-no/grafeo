package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.FactComment;
import no.mnemonic.services.grafeo.api.request.v1.GetFactCommentsRequest;
import no.mnemonic.services.grafeo.api.service.v1.StreamingResultSet;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.FactCommentResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactRequestResolver;

import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class FactGetCommentsDelegate implements Delegate {

  private final GrafeoSecurityContext securityContext;
  private final FactRequestResolver factRequestResolver;
  private final FactCommentResponseConverter factCommentResponseConverter;

  @Inject
  public FactGetCommentsDelegate(GrafeoSecurityContext securityContext,
                                 FactRequestResolver factRequestResolver,
                                 FactCommentResponseConverter factCommentResponseConverter) {
    this.securityContext = securityContext;
    this.factRequestResolver = factRequestResolver;
    this.factCommentResponseConverter = factCommentResponseConverter;
  }

  public ResultSet<FactComment> handle(GetFactCommentsRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch Fact and verify that it exists.
    FactRecord fact = factRequestResolver.resolveFact(request.getFact());
    // Verify that user is allowed to access the Fact.
    securityContext.checkReadPermission(fact);
    // Verify that user is allowed to view the Fact's comments.
    securityContext.checkPermission(FunctionConstants.viewGrafeoFactComment, fact.getOrganizationID());
    // Fetch comments for Fact and filter by 'before' and 'after' timestamps.
    List<FactComment> comments = ListUtils.list(fact.getComments())
            .stream()
            .filter(comment -> request.getBefore() == null || comment.getTimestamp() < request.getBefore())
            .filter(comment -> request.getAfter() == null || comment.getTimestamp() > request.getAfter())
            .map(factCommentResponseConverter)
            .collect(Collectors.toList());

    return StreamingResultSet.<FactComment>builder()
            .setCount(comments.size())
            .setLimit(0)
            .setValues(comments)
            .build();
  }
}
