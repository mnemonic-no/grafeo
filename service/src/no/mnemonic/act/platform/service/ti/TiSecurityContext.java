package no.mnemonic.act.platform.service.ti;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.auth.IdentityResolver;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.dao.cassandra.entity.AccessMode;
import no.mnemonic.act.platform.dao.cassandra.entity.FactAclEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.model.Credentials;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Specific SecurityContext for the ThreatIntelligenceService.
 */
public class TiSecurityContext extends SecurityContext {

  private final Function<UUID, List<FactAclEntity>> aclResolver;

  private TiSecurityContext(AccessController accessController, IdentityResolver identityResolver,
                            OrganizationResolver organizationResolver, SubjectResolver subjectResolver,
                            Credentials credentials, Function<UUID, List<FactAclEntity>> aclResolver) {
    super(accessController, identityResolver, organizationResolver, subjectResolver, credentials);
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

  /**
   * Check if a user is allowed to view a specific Fact based on the Fact's AccessMode.
   *
   * @param fact Fact to verify access to.
   * @return True if user has access to the Fact.
   */
  public boolean hasReadPermission(FactEntity fact) {
    try {
      checkReadPermission(fact);
      return true;
    } catch (AccessDeniedException | AuthenticationFailedException ignored) {
      return false;
    }
  }

  private boolean isInAcl(FactEntity fact) {
    List<FactAclEntity> acl = aclResolver.apply(fact.getId());
    return !CollectionUtils.isEmpty(acl) && acl.stream().anyMatch(entry -> getCurrentUserID().equals(entry.getSubjectID()));
  }

  public static class Builder {
    private AccessController accessController;
    private IdentityResolver identityResolver;
    private OrganizationResolver organizationResolver;
    private SubjectResolver subjectResolver;
    private Credentials credentials;
    private Function<UUID, List<FactAclEntity>> aclResolver;

    private Builder() {
    }

    public TiSecurityContext build() {
      ObjectUtils.notNull(aclResolver, "'aclResolver' not set in SecurityContext.");
      return new TiSecurityContext(accessController, identityResolver, organizationResolver, subjectResolver, credentials, aclResolver);
    }

    public Builder setAccessController(AccessController accessController) {
      this.accessController = accessController;
      return this;
    }

    public Builder setIdentityResolver(IdentityResolver identityResolver) {
      this.identityResolver = identityResolver;
      return this;
    }

    public Builder setOrganizationResolver(OrganizationResolver organizationResolver) {
      this.organizationResolver = organizationResolver;
      return this;
    }

    public Builder setSubjectResolver(SubjectResolver subjectResolver) {
      this.subjectResolver = subjectResolver;
      return this;
    }

    public Builder setCredentials(Credentials credentials) {
      this.credentials = credentials;
      return this;
    }

    public Builder setAclResolver(Function<UUID, List<FactAclEntity>> aclResolver) {
      this.aclResolver = aclResolver;
      return this;
    }
  }

}
