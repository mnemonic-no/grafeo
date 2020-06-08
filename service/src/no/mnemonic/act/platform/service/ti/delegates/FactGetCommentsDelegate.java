package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.FactComment;
import no.mnemonic.act.platform.api.request.v1.GetFactCommentsRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.FactCommentResponseConverter;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactRequestResolver;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class FactGetCommentsDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final FactRequestResolver factRequestResolver;
  private final FactCommentResponseConverter factCommentResponseConverter;

  @Inject
  public FactGetCommentsDelegate(TiSecurityContext securityContext,
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
    securityContext.checkPermission(TiFunctionConstants.viewThreatIntelFactComment, fact.getOrganizationID());
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
