package no.mnemonic.act.platform.auth;

import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;

import java.util.UUID;

/**
 * Common interface defining functions to resolve Subjects. Note that Subjects here are ACT model objects (see {@link Subject}).
 *
 * This interface can be implemented by an AccessController implementation or by another service able to resolve Subjects.
 * Whatever implementation is chosen an implementation must be provided when using a non-standard AccessController implementation.
 */
public interface SubjectResolver {

  /**
   * Resolves a Subject by its UUID. Returns NULL if no Subject with the given UUID exists.
   *
   * @param id Subject's unique ID
   * @return Resolved Subject
   */
  Subject resolveSubject(UUID id);

  /**
   * Resolves a Subject by its name. Returns NULL if no Subject with the given name exists.
   *
   * @param name Subject's name
   * @return Resolved Subject
   */
  Subject resolveSubject(String name);

  /**
   * Resolves the current user as a Subject from given credentials.
   *
   * @param credentials Credentials identifying the current user
   * @return Current user as a Subject
   * @throws InvalidCredentialsException Thrown if the given credentials are invalid.
   */
  Subject resolveCurrentUser(Credentials credentials) throws InvalidCredentialsException;

}
