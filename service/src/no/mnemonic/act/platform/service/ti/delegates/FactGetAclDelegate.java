package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.AclEntry;
import no.mnemonic.act.platform.api.request.v1.GetFactAclRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.AclEntryResponseConverter;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactRequestResolver;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;
import java.util.List;

public class FactGetAclDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final FactRequestResolver factRequestResolver;
  private final AclEntryResponseConverter aclEntryResponseConverter;

  @Inject
  public FactGetAclDelegate(TiSecurityContext securityContext,
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
    securityContext.checkPermission(TiFunctionConstants.viewThreatIntelFactAccess, fact.getOrganizationID());
    // Fetch ACL for Fact.
    List<AclEntry> acl = ListUtils.list(fact.getAcl(), aclEntryResponseConverter);

    return StreamingResultSet.<AclEntry>builder()
            .setCount(acl.size())
            .setLimit(0)
            .setValues(acl)
            .build();
  }
}
