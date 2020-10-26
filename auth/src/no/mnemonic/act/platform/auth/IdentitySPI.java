package no.mnemonic.act.platform.auth;

import no.mnemonic.services.common.auth.model.FunctionIdentity;
import no.mnemonic.services.common.auth.model.OrganizationIdentity;
import no.mnemonic.services.common.auth.model.SessionDescriptor;
import no.mnemonic.services.common.auth.model.SubjectIdentity;

import java.util.UUID;

/**
 * Common interface defining how to map ACT-specific identifiers for Functions, Organizations and Subjects to
 * AccessController implementation-specific identities. Every AccessController implementation should also provide
 * such a resolver implementation in order to be able to easily switch out the implementation.
 */
public interface IdentitySPI {

  /**
   * Maps a Function identified by name to a FunctionIdentity.
   *
   * @param name Function name
   * @return Implementation-specific FunctionIdentity
   */
  FunctionIdentity resolveFunctionIdentity(String name);

  /**
   * Maps an Organization identified by UUID to an OrganizationIdentity.
   *
   * @param id Organization UUID
   * @return Implementation-specific OrganizationIdentity
   */
  OrganizationIdentity resolveOrganizationIdentity(UUID id);

  /**
   * Maps an OrganizationIdentity to the UUID of an Organization.
   *
   * @param identity Implementation-specific OrganizationIdentity
   * @return Organization UUID
   */
  UUID resolveOrganizationUUID(OrganizationIdentity identity);

  /**
   * Maps a Subject identified by UUID to a SubjectIdentity.
   *
   * @param id Subject UUID
   * @return Implementation-specific SubjectIdentity
   */
  SubjectIdentity resolveSubjectIdentity(UUID id);

  /**
   * Maps a SubjectIdentity to the UUID of a Subject.
   *
   * @param identity Implementation-specific SubjectIdentity
   * @return Subject UUID
   */
  UUID resolveSubjectUUID(SubjectIdentity identity);

  /**
   * Maps a SessionDescriptor to the UUID of the Subject identified by the given descriptor.
   *
   * @param descriptor Implementation-specific SessionDescriptor
   * @return Subject UUID
   */
  UUID resolveSubjectUUID(SessionDescriptor descriptor);

}
