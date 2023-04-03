package no.mnemonic.services.grafeo.service.implementation.resolvers.response;

import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.grafeo.api.model.v1.Subject;
import no.mnemonic.services.grafeo.auth.ServiceAccountSPI;
import no.mnemonic.services.grafeo.auth.SubjectSPI;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class SubjectByIdResponseResolver implements Function<UUID, Subject> {

  private static final Logger LOGGER = Logging.getLogger(SubjectByIdResponseResolver.class);

  private final SubjectSPI subjectResolver;
  private final ServiceAccountSPI credentialsResolver;
  private final Map<UUID, Subject> responseCache;

  @Inject
  public SubjectByIdResponseResolver(SubjectSPI subjectResolver,
                                     ServiceAccountSPI credentialsResolver,
                                     Map<UUID, Subject> responseCache) {
    this.subjectResolver = subjectResolver;
    this.credentialsResolver = credentialsResolver;
    this.responseCache = responseCache;
  }

  @Override
  public Subject apply(UUID id) {
    if (id == null) return null;
    return responseCache.computeIfAbsent(id, this::resolveUncached);
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
