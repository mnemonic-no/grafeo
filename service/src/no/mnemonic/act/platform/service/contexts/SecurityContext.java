package no.mnemonic.act.platform.service.contexts;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.UnexpectedAuthenticationFailedException;
import no.mnemonic.act.platform.auth.IdentitySPI;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.common.auth.model.NamedFunction;
import no.mnemonic.services.common.auth.model.OrganizationIdentity;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The SecurityContext provides methods to perform access control checks, e.g. if a user is allowed to perform
 * a specific operation or if a user has access to a specific object.
 */
public abstract class SecurityContext implements AutoCloseable {

  private static final ThreadLocal<SecurityContext> currentContext = new ThreadLocal<>();
  private final AccessController accessController;
  private final IdentitySPI identityResolver;
  private final Credentials credentials;

  protected SecurityContext(AccessController accessController, IdentitySPI identityResolver, Credentials credentials) {
    this.accessController = ObjectUtils.notNull(accessController, "'accessController' not set in SecurityContext.");
    this.identityResolver = ObjectUtils.notNull(identityResolver, "'identityResolver' not set in SecurityContext.");
    this.credentials = ObjectUtils.notNull(credentials, "'credentials' not set in SecurityContext.");
  }

  /**
   * Retrieve the current SecurityContext.
   *
   * @return Current SecurityContext
   * @throws IllegalStateException If SecurityContext is not set.
   */
  public static SecurityContext get() {
    if (currentContext.get() == null) throw new IllegalStateException("Security context not set.");
    return currentContext.get();
  }

  /**
   * Set a new SecurityContext (but it is not allowed to override an existing SecurityContext).
   *
   * @param ctx New SecurityContext
   * @return New SecurityContext
   * @throws IllegalStateException If SecurityContext is already set.
   */
  public static SecurityContext set(SecurityContext ctx) {
    if (currentContext.get() != null) throw new IllegalStateException("Security context already set.");
    currentContext.set(ctx);
    return ctx;
  }

  /**
   * Determine if SecurityContext is set.
   *
   * @return True, if SecurityContext is set.
   */
  public static boolean isSet() {
    return currentContext.get() != null;
  }

  /**
   * Remove the current SecurityContext.
   * <p>
   * Use with caution as many parts of the code rely on a set SecurityContext. This method is mainly useful for testing.
   *
   * @return Current SecurityContext, or NULL if it was not set.
   */
  public static SecurityContext clear() {
    SecurityContext oldCtx = currentContext.get();
    currentContext.remove();
    return oldCtx;
  }

  @Override
  public void close() {
    currentContext.remove();
  }

  /**
   * Return the user's credentials stored in SecurityContext.
   *
   * @return Stored credentials
   */
  public Credentials getCredentials() {
    return credentials;
  }

  /**
   * Check if a user is allowed to perform a specific operation.
   *
   * @param function Operation the user wants to perform.
   * @throws AccessDeniedException         If the user is not allowed to perform the operation.
   * @throws AuthenticationFailedException If the user could not be authenticated.
   */
  public void checkPermission(NamedFunction function) throws AccessDeniedException, AuthenticationFailedException {
    try {
      //noinspection unchecked
      if (!accessController.hasPermission(credentials, function)) {
        throw new AccessDeniedException(String.format("User is not allowed to perform operation '%s'.", function.getName()));
      }
    } catch (InvalidCredentialsException ex) {
      throw new AuthenticationFailedException("Could not authenticate user: " + ex.getMessage());
    }
  }

  /**
   * Check if a user is allowed to perform a specific operation for data of a specified Organization.
   *
   * @param function       Operation the user wants to perform.
   * @param organizationID Organization which data the user wants to access (identified by ID).
   * @throws AccessDeniedException         If the user is not allowed to perform the operation.
   * @throws AuthenticationFailedException If the user could not be authenticated.
   */
  public void checkPermission(NamedFunction function, UUID organizationID) throws AccessDeniedException, AuthenticationFailedException {
    try {
      //noinspection unchecked
      if (!accessController.hasPermission(credentials, function, identityResolver.resolveOrganizationIdentity(organizationID))) {
        throw new AccessDeniedException(String.format("User is not allowed to perform operation '%s' for organization '%s'.", function.getName(), organizationID));
      }
    } catch (InvalidCredentialsException ex) {
      throw new AuthenticationFailedException("Could not authenticate user: " + ex.getMessage());
    }
  }

  /**
   * Return the ID of the current user.
   *
   * @return ID of current user
   */
  public UUID getCurrentUserID() {
    try {
      //noinspection unchecked
      return identityResolver.resolveSubjectUUID(accessController.validate(credentials));
    } catch (InvalidCredentialsException ex) {
      // getCurrentUserID() should only be called in a context with an already authenticated user.
      throw new UnexpectedAuthenticationFailedException("Could not authenticate user: " + ex.getMessage());
    }
  }

  /**
   * Return the IDs of the Organizations the current user has access to.
   *
   * @return IDs of available Organizations
   */
  public Set<UUID> getAvailableOrganizationID() {
    try {
      //noinspection unchecked
      Set<OrganizationIdentity> organizations = accessController.getAvailableOrganizations(credentials);
      return organizations.stream()
              .map(identityResolver::resolveOrganizationUUID)
              .collect(Collectors.toSet());
    } catch (InvalidCredentialsException ex) {
      // getAvailableOrganizations() should only be called in a context with an already authenticated user.
      throw new UnexpectedAuthenticationFailedException("Could not authenticate user: " + ex.getMessage());
    }
  }

}
