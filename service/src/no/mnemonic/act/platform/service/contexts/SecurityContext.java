package no.mnemonic.act.platform.service.contexts;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.UnexpectedAuthenticationFailedException;
import no.mnemonic.act.platform.auth.IdentityResolver;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.common.auth.model.NamedFunction;

import java.util.UUID;

/**
 * The SecurityContext provides methods to perform access control checks, e.g. if a user is allowed to perform
 * a specific operation or if a user has access to a specific object.
 */
public abstract class SecurityContext implements AutoCloseable {

  private static final ThreadLocal<SecurityContext> currentContext = new ThreadLocal<>();
  private final AccessController accessController;
  private final IdentityResolver identityResolver;
  private final OrganizationResolver organizationResolver;
  private final SubjectResolver subjectResolver;
  private final Credentials credentials;

  protected SecurityContext(AccessController accessController, IdentityResolver identityResolver,
                            OrganizationResolver organizationResolver, SubjectResolver subjectResolver,
                            Credentials credentials) {
    this.accessController = ObjectUtils.notNull(accessController, "'accessController' not set in SecurityContext.");
    this.identityResolver = ObjectUtils.notNull(identityResolver, "'identityResolver' not set in SecurityContext.");
    this.organizationResolver = ObjectUtils.notNull(organizationResolver, "'organizationResolver' not set in SecurityContext.");
    this.subjectResolver = ObjectUtils.notNull(subjectResolver, "'subjectResolver' not set in SecurityContext.");
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
  public void close() throws Exception {
    currentContext.remove();
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
      return subjectResolver.resolveCurrentUser(credentials).getId();
    } catch (InvalidCredentialsException ex) {
      // getCurrentUserID() should only be called in a context with an already authenticated user.
      throw new UnexpectedAuthenticationFailedException("Could not authenticate user: " + ex.getMessage());
    }
  }

  /**
   * Return the ID of the current user's organization.
   *
   * @return ID of current user's organization
   */
  public UUID getCurrentUserOrganizationID() {
    try {
      return organizationResolver.resolveCurrentUserAffiliation(credentials).getId();
    } catch (InvalidCredentialsException ex) {
      // getCurrentUserOrganizationID() should only be called in a context with an already authenticated user.
      throw new UnexpectedAuthenticationFailedException("Could not authenticate user: " + ex.getMessage());
    }
  }

}
