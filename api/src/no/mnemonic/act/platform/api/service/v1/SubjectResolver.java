package no.mnemonic.act.platform.api.service.v1;

import no.mnemonic.act.platform.api.model.v1.Subject;

import java.util.UUID;

/**
 * Interface defining a function to resolve a Subject by its UUID.
 */
public interface SubjectResolver {

  /**
   * Resolves a Subject by its UUID.
   *
   * @param id Subject's unique ID
   * @return Resolved Subject
   */
  Subject resolveSubject(UUID id);

}
