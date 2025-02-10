package no.mnemonic.services.grafeo.seb.producer.v1.resolvers;

import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.grafeo.api.model.v1.Subject;
import no.mnemonic.services.grafeo.auth.ServiceAccountSPI;
import no.mnemonic.services.grafeo.auth.SubjectSPI;
import no.mnemonic.services.grafeo.seb.model.v1.SubjectInfoSEB;

import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class SubjectInfoServiceAccountResolver implements Function<UUID, SubjectInfoSEB> {

  private static final Logger LOGGER = Logging.getLogger(SubjectInfoServiceAccountResolver.class);

  private final SubjectSPI subjectResolver;
  private final ServiceAccountSPI credentialsResolver;
  // This utilizes the same cache as SubjectByIdResponseResolver as both resolvers are bound to the service account.
  private final Map<UUID, Subject> subjectCache;

  @Inject
  public SubjectInfoServiceAccountResolver(SubjectSPI subjectResolver,
                                           ServiceAccountSPI credentialsResolver,
                                           Map<UUID, Subject> subjectCache) {
    this.subjectResolver = subjectResolver;
    this.credentialsResolver = credentialsResolver;
    this.subjectCache = subjectCache;
  }

  @Override
  public SubjectInfoSEB apply(UUID id) {
    if (id == null) return null;

    Subject subject = subjectCache.computeIfAbsent(id, this::resolveUncached);
    return SubjectInfoSEB.builder()
            .setId(subject.getId())
            .setName(subject.getName())
            .build();
  }

  private Subject resolveUncached(UUID id) {
    return ObjectUtils.ifNull(resolveSubject(id), Subject.builder()
            .setId(id)
            .setName("N/A")
            .build()
    );
  }

  private Subject resolveSubject(UUID id) {
    try {
      return subjectResolver.resolveSubject(credentialsResolver.get(), id);
    } catch (InvalidCredentialsException ex) {
      LOGGER.warning(ex, "Could not resolve Subject for id = %s.", id);
      return null;
    }
  }
}
