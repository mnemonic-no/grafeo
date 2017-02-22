package no.mnemonic.act.platform.service.contexts;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;

import java.util.UUID;

/**
 * The SecurityContext provides methods to perform access control checks, e.g. if a user is allowed to perform
 * a specific operation or if a user has access to a specific object.
 */
public class SecurityContext implements AutoCloseable {

  private static final ThreadLocal<SecurityContext> currentContext = new ThreadLocal<>();

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
    // NOOP for now, delegate to access controller later.
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
    // NOOP for now, delegate to access controller later.
  }

  /**
   * Return the ID of the current user.
   *
   * @return ID of current user
   */
  public UUID getCurrentUserID() {
    // Return a static user ID for now.
    return UUID.fromString("00000000-0000-0000-0000-000000000000");
  }

  /**
   * Return the ID of the current user's organization.
   *
   * @return ID of current user's organization
   */
  public UUID getCurrentUserOrganizationID() {
    // Return a static organization ID for now.
    return UUID.fromString("00000000-0000-0000-0000-000000000000");
  }

  /**
   * For now define this interface here. Needs to be refactored when we implement real access control.
   */
  public interface NamedFunction {
    String getName();
  }

}
