package no.mnemonic.act.platform.service.ti.helpers;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactCommentRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.resolvers.OriginResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.MapUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.mnemonic.act.platform.service.ti.ThreatIntelligenceServiceImpl.GLOBAL_NAMESPACE;
import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;

public class FactCreateHelper {

  private static final float ORIGIN_DEFAULT_TRUST = 0.8f;
  private static final Map<FactRecord.AccessMode, Integer> ACCESS_MODE_ORDER = MapUtils.map(
          T(FactRecord.AccessMode.Public, 0),
          T(FactRecord.AccessMode.RoleBased, 1),
          T(FactRecord.AccessMode.Explicit, 2)
  );

  private final TiSecurityContext securityContext;
  private final SubjectResolver subjectResolver;
  private final OrganizationResolver organizationResolver;
  private final OriginResolver originResolver;
  private final OriginManager originManager;

  @Inject
  public FactCreateHelper(TiSecurityContext securityContext,
                          SubjectResolver subjectResolver,
                          OrganizationResolver organizationResolver,
                          OriginResolver originResolver,
                          OriginManager originManager) {
    this.securityContext = securityContext;
    this.subjectResolver = subjectResolver;
    this.organizationResolver = organizationResolver;
    this.originResolver = originResolver;
    this.originManager = originManager;
  }

  /**
   * Resolve an Organization by its ID. Falls back to the Organization of the given Origin if no 'organizationID' is provided.
   * If neither 'organizationID' is provided nor the given Origin is linked to an Organization the current user's Organization
   * will be returned.
   *
   * @param organizationID ID of Organization (can be null)
   * @param fallback       Origin used as fallback (can be null)
   * @return Resolved Organization (will never be null)
   * @throws AccessDeniedException         Thrown if the current user does not have access to the resolved Organization
   * @throws AuthenticationFailedException Thrown if the current user could not be authenticated
   * @throws InvalidArgumentException      Thrown if an Organization cannot be resolved
   */
  public Organization resolveOrganization(UUID organizationID, OriginEntity fallback)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    if (organizationID != null) {
      // If an Organization was provided in the request fetch it.
      Organization organization = fetchOrganization(organizationID);
      // Make sure that user has access to the resolved Organization.
      securityContext.checkReadPermission(organization);
      return organization;
    }

    if (fallback != null && fallback.getOrganizationID() != null) {
      // If an Organization wasn't provided in the request fall back to the Origin's Organization. If the user has access to
      // the Origin (which must be checked by the caller of this method!) then one also has access to the Origin's Organization.
      return fetchOrganization(fallback.getOrganizationID());
    }

    // As a last resort fall back to the current user's Organization.
    return fetchOrganization(securityContext.getCurrentUserOrganizationID());
  }

  /**
   * Resolve an Origin by its ID. Falls back to the current user if no 'originID' is provided.
   * This will create a new Origin for the current user if no Origin exists yet.
   *
   * @param originID ID of Origin (can be null)
   * @return Resolved Origin (will never be null)
   * @throws AccessDeniedException         Thrown if the current user does not have access to the resolved Origin
   * @throws AuthenticationFailedException Thrown if the current user could not be authenticated
   * @throws InvalidArgumentException      Thrown if 'originID' is provided and the Origin does not exist, or the resolved Origin is deleted
   */
  public OriginEntity resolveOrigin(UUID originID)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    if (originID != null) {
      // If an Origin was provided in the request fetch it or throw an exception.
      OriginEntity origin = originResolver.apply(originID);
      if (origin != null) {
        // Make sure that user has access to the resolved Origin.
        securityContext.checkReadPermission(origin);
        return assertNotDeleted(origin);
      }

      throw new InvalidArgumentException()
              .addValidationError("Origin does not exist.", "origin.not.exist", "origin", originID.toString());
    }

    // If an Origin wasn't provided in the request fall back to the current user as Origin.
    // Note that the current user always has access to its own Origin.
    OriginEntity origin = originResolver.apply(securityContext.getCurrentUserID());
    if (origin != null) return assertNotDeleted(origin);

    // Create an Origin for the current user if it doesn't exist yet.
    Subject currentUser = subjectResolver.resolveSubject(securityContext.getCurrentUserID());
    return originManager.saveOrigin(new OriginEntity()
            .setId(securityContext.getCurrentUserID())
            .setNamespaceID(GLOBAL_NAMESPACE) // For now everything will just be part of the global namespace.
            .setOrganizationID(ObjectUtils.ifNotNull(currentUser.getOrganization(), Organization.Info::getId))
            .setName(createOriginName(currentUser))
            .setTrust(ORIGIN_DEFAULT_TRUST)
            .setType(OriginEntity.Type.User)
    );
  }

  /**
   * Resolve AccessMode from a request and verify that it is not less restrictive than the AccessMode from another Fact.
   * Falls back to AccessMode from referenced Fact if requested AccessMode is not given.
   *
   * @param referencedFact      Fact to validate AccessMode against
   * @param requestedAccessMode Requested AccessMode (can be null)
   * @return Resolved AccessMode
   * @throws InvalidArgumentException Thrown if requested AccessMode is less restrictive than AccessMode of referenced Fact
   */
  public FactRecord.AccessMode resolveAccessMode(FactRecord referencedFact, AccessMode requestedAccessMode) throws InvalidArgumentException {
    if (referencedFact == null || referencedFact.getAccessMode() == null) return null;

    // If no AccessMode provided fall back to the AccessMode from the referenced Fact.
    FactRecord.AccessMode mode = ObjectUtils.ifNotNull(requestedAccessMode, m -> FactRecord.AccessMode.valueOf(m.name()), referencedFact.getAccessMode());

    // The requested AccessMode of a new Fact should not be less restrictive than the AccessMode of the referenced Fact.
    if (ACCESS_MODE_ORDER.get(mode) < ACCESS_MODE_ORDER.get(referencedFact.getAccessMode())) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("Requested AccessMode cannot be less restrictive than AccessMode of Fact with id = %s.", referencedFact.getId()),
                      "access.mode.too.wide", "accessMode", mode.name());
    }

    return mode;
  }

  /**
   * Add additional ACL entries to a FactRecord. It will omit any entries already in the Fact's ACL
   * and makes sure that the current user is in the ACL with AccessMode 'Explicit'.
   *
   * @param fact Fact the ACL belongs to
   * @param acl  List of Subject IDs
   * @return Fact with updated ACL
   */
  public FactRecord withAcl(FactRecord fact, List<UUID> acl) {
    if (fact == null) return null;
    // It doesn't make sense to have an ACL when Fact is public.
    if (fact.getAccessMode() == FactRecord.AccessMode.Public) return fact;

    // Fetch any existing entries ...
    Set<UUID> existingAcl = SetUtils.set(fact.getAcl())
            .stream()
            .map(FactAclEntryRecord::getSubjectID)
            .collect(Collectors.toSet());
    // ... and make sure not to add any duplicates.
    Set<UUID> subjectsToAdd = SetUtils.set(acl)
            .stream()
            .filter(subject -> !existingAcl.contains(subject))
            .collect(Collectors.toSet());

    UUID currentUser = securityContext.getCurrentUserID();
    if (fact.getAccessMode() == FactRecord.AccessMode.Explicit && !existingAcl.contains(currentUser)) {
      // Make sure that current user is in the ACL with 'Explicit' AccessMode.
      // With 'RoleBased' AccessMode current user has access to Fact via the Organization.
      subjectsToAdd.add(currentUser);
    }

    // Add all new ACL entries to Fact.
    for (UUID subject : subjectsToAdd) {
      fact.addAclEntry(new FactAclEntryRecord()
              .setId(UUID.randomUUID())
              .setOriginID(fact.getOriginID())
              .setSubjectID(subject)
              .setTimestamp(System.currentTimeMillis())
      );
    }

    return fact;
  }

  /**
   * Add an additional comment to a FactRecord.
   *
   * @param fact    Fact the comment belongs to
   * @param comment Comment to add
   * @return Fact with added comment
   */
  public FactRecord withComment(FactRecord fact, String comment) {
    if (fact == null) return null;
    if (StringUtils.isBlank(comment)) return fact;

    return fact.addComment(new FactCommentRecord()
            .setId(UUID.randomUUID())
            .setOriginID(fact.getOriginID())
            .setComment(comment)
            .setTimestamp(System.currentTimeMillis()));
  }

  private Organization fetchOrganization(UUID organizationID) throws InvalidArgumentException {
    Organization organization = organizationResolver.resolveOrganization(organizationID);
    if (organization != null) return organization;

    throw new InvalidArgumentException()
            .addValidationError("Organization does not exist.", "organization.not.exist", "organization", organizationID.toString());
  }

  private OriginEntity assertNotDeleted(OriginEntity origin) throws InvalidArgumentException {
    // It's not allowed to use a deleted Origin for a new Fact.
    if (SetUtils.set(origin.getFlags()).contains(OriginEntity.Flag.Deleted)) {
      throw new InvalidArgumentException()
              .addValidationError("Not allowed to create a Fact using a deleted Origin.",
                      "invalid.origin.deleted", "origin", origin.getId().toString());
    }

    return origin;
  }

  private String createOriginName(Subject currentUser) {
    // There can't exist two Origins with the same name. Avoid collisions by generating a unique name if necessary.
    OriginEntity collision = originManager.getOrigin(currentUser.getName());
    return collision == null ? currentUser.getName() : String.format("%s (%s)", currentUser.getName(), UUID.randomUUID().toString().substring(0, 8));
  }
}
