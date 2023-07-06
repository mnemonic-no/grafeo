package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.AclEntry;
import no.mnemonic.services.grafeo.api.request.v1.GetFactAclRequest;
import no.mnemonic.services.grafeo.api.service.v1.StreamingResultSet;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.AclEntryResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactRequestResolver;

import javax.inject.Inject;
import java.util.List;

public class FactGetAclDelegate implements Delegate {

  private final GrafeoSecurityContext securityContext;
  private final FactRequestResolver factRequestResolver;
  private final AclEntryResponseConverter aclEntryResponseConverter;

  @Inject
  public FactGetAclDelegate(GrafeoSecurityContext securityContext,
                            FactRequestResolver factRequestResolver,
                            AclEntryResponseConverter aclEntryResponseConverter) {
    this.securityContext = securityContext;
    this.factRequestResolver = factRequestResolver;
    this.aclEntryResponseConverter = aclEntryResponseConverter;
  }

  public ResultSet<AclEntry> handle(GetFactAclRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch Fact and verify that it exists.
    FactRecord fact = factRequestResolver.resolveFact(request.getFact());
    // Verify that user is allowed to access the Fact.
    securityContext.checkReadPermission(fact);
    // Verify that user is allowed to view the Fact's ACL.
    securityContext.checkPermission(FunctionConstants.viewGrafeoFactAccess, fact.getOrganizationID());
    // Fetch ACL for Fact.
    List<AclEntry> acl = ListUtils.list(fact.getAcl(), aclEntryResponseConverter);

    return StreamingResultSet.<AclEntry>builder()
            .setCount(acl.size())
            .setLimit(0)
            .setValues(acl)
            .build();
  }
}
