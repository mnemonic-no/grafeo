package no.mnemonic.services.grafeo.service.implementation;

import com.google.common.collect.Streams;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.model.v1.Organization;
import no.mnemonic.services.grafeo.auth.IdentitySPI;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.cassandra.entity.AccessMode;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactAclEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.service.contexts.SecurityContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Specific SecurityContext for the GrafeoService.
 */
public class GrafeoSecurityContext extends SecurityContext {

  private final ObjectFactDao objectFactDao;
  private final Function<UUID, List<FactAclEntity>> aclResolver;

  private GrafeoSecurityContext(AccessController accessController,
                                IdentitySPI identityResolver,
                                Credentials credentials,
                                ObjectFactDao objectFactDao,
                                Function<UUID, List<FactAclEntity>> aclResolver) {
    super(accessController, identityResolver, credentials);
    this.objectFactDao = objectFactDao;
    this.aclResolver = aclResolver;
  }

  public static GrafeoSecurityContext get() {
    return (GrafeoSecurityContext) SecurityContext.get();
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
   * @deprecated Use {@link #checkReadPermission(FactRecord)} instead.
   */
  @Deprecated
  public void checkReadPermission(FactEntity fact) throws AccessDeniedException, AuthenticationFailedException {
    if (fact == null) throw new AccessDeniedException("No access to Fact.");

    if (fact.getAccessMode() == AccessMode.Public) {
      // Only verify that user has general permission to view Facts.
      checkPermission(FunctionConstants.viewGrafeoFact);
      // Access allowed because user is generally allowed to view Facts.
      return;
    }

    if (isInAcl(fact)) {
      // Access allowed because user is in the Fact's ACL, either granted directly or to a parent group.
      return;
    }

    if (fact.getAccessMode() == AccessMode.Explicit) {
      // User is not in ACL of the Fact but explicit access is required.
      throw new AccessDeniedException(String.format("No access to Fact with id = %s.", fact.getId()));
    }

    // Fallback to role-based access control and verify that user has access to Facts of a specific organization.
    // This also catches the case where AccessMode == RoleBased and user is not in the Fact's ACL.
    checkPermission(FunctionConstants.viewGrafeoFact, fact.getOrganizationID());
  }

  /**
   * Check if a user is allowed to view a specific Fact based on the Fact's AccessMode.
   *
   * @param fact Fact to verify access to.
   * @throws AccessDeniedException         If the user is not allowed to view the Fact.
   * @throws AuthenticationFailedException If the user could not be authenticated.
   */
  public void checkReadPermission(FactRecord fact) throws AccessDeniedException, AuthenticationFailedException {
    if (fact == null) throw new AccessDeniedException("No access to Fact.");

    if (fact.getAccessMode() == FactRecord.AccessMode.Public) {
      // Only verify that user has general permission to view Facts.
      checkPermission(FunctionConstants.viewGrafeoFact);
      // Access allowed because user is generally allowed to view Facts.
      return;
    }

    if (!CollectionUtils.isEmpty(fact.getAcl()) &&
            fact.getAcl().stream().anyMatch(entry -> getCurrentUserIdentities().contains(entry.getSubjectID()))) {
      // Access allowed because user is in the Fact's ACL, either granted directly or to a parent group.
      return;
    }

    if (fact.getAccessMode() == FactRecord.AccessMode.Explicit) {
      // User is not in ACL of the Fact but explicit access is required.
      throw new AccessDeniedException(String.format("No access to Fact with id = %s.", fact.getId()));
    }

    // Fallback to role-based access control and verify that user has access to Facts of a specific organization.
    // This also catches the case where AccessMode == RoleBased and user is not in the Fact's ACL.
    checkPermission(FunctionConstants.viewGrafeoFact, fact.getOrganizationID());
  }

  /**
   * Check if a user is allowed to view a specific Object. The user needs access to at least one Fact bound to the Object.
   *
   * @param object Object to verify access to.
   * @throws AccessDeniedException         If the user is not allowed to view the Object.
   * @throws AuthenticationFailedException If the user could not be authenticated.
   */
  public void checkReadPermission(ObjectRecord object) throws AccessDeniedException, AuthenticationFailedException {
    if (object == null) {
      // User should not get a different response if an Object is not in the system or if user does not have access to it.
      throw new AccessDeniedException("No access to Object.");
    }

    // Iterate through all bound Facts and return the first accessible Fact.
    // The user needs access to at least one bound Fact to have access to the Object.
    Optional<FactRecord> accessibleFact = Streams.stream(objectFactDao.retrieveObjectFacts(object.getId()))
            .filter(this::hasReadPermission)
            .findFirst();
    if (!accessibleFact.isPresent()) {
      // User does not have access to any Facts bound to this Object.
      throw new AccessDeniedException("No access to Object.");
    }
  }

  /**
   * Check if a user is allowed to view a specific Origin.
   *
   * @param origin Origin to verify access to.
   * @throws AccessDeniedException         If the user is not allowed to view the Origin.
   * @throws AuthenticationFailedException If the user could not be authenticated.
   */
  public void checkReadPermission(OriginEntity origin) throws AccessDeniedException, AuthenticationFailedException {
    if (origin == null) throw new AccessDeniedException("No access to Origin.");

    if (origin.getOrganizationID() != null) {
      // Check that the user has view permission for the organization the Origin belongs to.
      checkPermission(FunctionConstants.viewGrafeoOrigin, origin.getOrganizationID());
    } else {
      // Only check that the user has general view permission.
      checkPermission(FunctionConstants.viewGrafeoOrigin);
    }
  }

  /**
   * Check if a user is allowed to view a specific Organization.
   *
   * @param organization Organization to verify access to.
   * @throws AccessDeniedException         If the user is not allowed to view the Organization.
   * @throws AuthenticationFailedException If the user could not be authenticated.
   */
  public void checkReadPermission(Organization organization) throws AccessDeniedException, AuthenticationFailedException {
    if (organization == null) throw new AccessDeniedException("No access to Organization.");

    // Just check that the Organization is available to the current user.
    if (!getAvailableOrganizationID().contains(organization.getId())) {
      throw new AccessDeniedException(String.format("No access to Organization with id = %s.", organization.getId()));
    }
  }

  /**
   * Check if a user is allowed to view a specific Fact based on the Fact's AccessMode.
   *
   * @param fact Fact to verify access to.
   * @return True if user has access to the Fact.
   * @deprecated Use {@link #hasReadPermission(FactRecord)} instead.
   */
  @Deprecated
  public boolean hasReadPermission(FactEntity fact) {
    try {
      checkReadPermission(fact);
      return true;
    } catch (AccessDeniedException | AuthenticationFailedException ignored) {
      return false;
    }
  }

  /**
   * Check if a user is allowed to view a specific Fact based on the Fact's AccessMode.
   *
   * @param fact Fact to verify access to.
   * @return True if user has access to the Fact.
   */
  public boolean hasReadPermission(FactRecord fact) {
    try {
      checkReadPermission(fact);
      return true;
    } catch (AccessDeniedException | AuthenticationFailedException ignored) {
      return false;
    }
  }

  /**
   * Check if a user is allowed to view a specific Object. The user needs access to at least one Fact bound to the Object.
   *
   * @param object Object to verify access to.
   * @return True if user has access to the Object.
   */
  public boolean hasReadPermission(ObjectRecord object) {
    try {
      checkReadPermission(object);
      return true;
    } catch (AccessDeniedException | AuthenticationFailedException ignored) {
      return false;
    }
  }

  /**
   * Check if a user is allowed to view a specific Origin.
   *
   * @param origin Origin to verify access to.
   * @return True if user has access to the Origin.
   */
  public boolean hasReadPermission(OriginEntity origin) {
    try {
      checkReadPermission(origin);
      return true;
    } catch (AccessDeniedException | AuthenticationFailedException ignored) {
      return false;
    }
  }

  /**
   * Check if a user is allowed to view a specific Organization.
   *
   * @param organization Organization to verify access to.
   * @return True if user has access to the Organization.
   */
  public boolean hasReadPermission(Organization organization) {
    try {
      checkReadPermission(organization);
      return true;
    } catch (AccessDeniedException | AuthenticationFailedException ignored) {
      return false;
    }
  }

  private boolean isInAcl(FactEntity fact) {
    List<FactAclEntity> acl = aclResolver.apply(fact.getId());
    return !CollectionUtils.isEmpty(acl) && acl.stream().anyMatch(entry -> getCurrentUserIdentities().contains(entry.getSubjectID()));
  }

  public static class Builder {
    private AccessController accessController;
    private IdentitySPI identityResolver;
    private Credentials credentials;
    private ObjectFactDao objectFactDao;
    private Function<UUID, List<FactAclEntity>> aclResolver;

    private Builder() {
    }

    public GrafeoSecurityContext build() {
      ObjectUtils.notNull(objectFactDao, "'objectFactDao' not set in SecurityContext.");
      ObjectUtils.notNull(aclResolver, "'aclResolver' not set in SecurityContext.");
      return new GrafeoSecurityContext(accessController, identityResolver, credentials, objectFactDao, aclResolver);
    }

    public Builder setAccessController(AccessController accessController) {
      this.accessController = accessController;
      return this;
    }

    public Builder setIdentityResolver(IdentitySPI identityResolver) {
      this.identityResolver = identityResolver;
      return this;
    }

    public Builder setCredentials(Credentials credentials) {
      this.credentials = credentials;
      return this;
    }

    public Builder setObjectFactDao(ObjectFactDao objectFactDao) {
      this.objectFactDao = objectFactDao;
      return this;
    }

    public Builder setAclResolver(Function<UUID, List<FactAclEntity>> aclResolver) {
      this.aclResolver = aclResolver;
      return this;
    }
  }

}
