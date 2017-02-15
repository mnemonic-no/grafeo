package no.mnemonic.act.platform.service.ti;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.entity.cassandra.AccessMode;
import no.mnemonic.act.platform.entity.cassandra.FactAclEntity;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Specific SecurityContext for the ThreatIntelligenceService.
 */
public class TiSecurityContext extends SecurityContext {

  private final Function<UUID, List<FactAclEntity>> aclResolver;

  private TiSecurityContext(Function<UUID, List<FactAclEntity>> aclResolver) {
    this.aclResolver = aclResolver;
  }

  public static TiSecurityContext get() {
    return (TiSecurityContext) SecurityContext.get();
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Check if a user is allowed to view a specific Fact based on the Fact's AccessMode.
   *
   * @param fact Fact to verify access to.
   * @throws AccessDeniedException         If the user is not allowed to view the Fact.
   * @throws AuthenticationFailedException If the user could not be authenticated.
   */
  public void checkReadPermission(FactEntity fact) throws AccessDeniedException, AuthenticationFailedException {
    if (fact == null) return;

    if (fact.getAccessMode() == AccessMode.Public) {
      // Only verify that user has general permission to view Facts.
      checkPermission(TiFunctionConstants.viewFactObjects);
      // Access allowed because user is generally allowed to view Facts.
      return;
    }

    if (isInAcl(fact)) {
      // Access allowed because user is in the Fact's ACL.
      return;
    }

    if (fact.getAccessMode() == AccessMode.Explicit) {
      // User is not in ACL of the Fact but explicit access is required.
      throw new AccessDeniedException(String.format("No access to Fact with id = %s.", fact.getId()));
    }

    // Fallback to role-based access control and verify that user has access to Facts of a specific organization.
    // This also catches the case where AccessMode == RoleBased and user is not in the Fact's ACL.
    checkPermission(TiFunctionConstants.viewFactObjects, fact.getOrganizationID());
  }

  private boolean isInAcl(FactEntity fact) {
    List<FactAclEntity> acl = aclResolver.apply(fact.getId());
    return !CollectionUtils.isEmpty(acl) && acl.stream().anyMatch(entry -> getCurrentUserID().equals(entry.getSubjectID()));
  }

  public static class Builder {
    private Function<UUID, List<FactAclEntity>> aclResolver;

    private Builder() {
    }

    public TiSecurityContext build() {
      ObjectUtils.notNull(aclResolver, "'aclResolver' not set in SecurityContext.");
      return new TiSecurityContext(aclResolver);
    }

    public Builder setAclResolver(Function<UUID, List<FactAclEntity>> aclResolver) {
      this.aclResolver = aclResolver;
      return this;
    }
  }

}
