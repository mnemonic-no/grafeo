package no.mnemonic.act.platform.seb.producer.v1.resolvers;

import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.ServiceAccountSPI;
import no.mnemonic.act.platform.auth.SubjectSPI;
import no.mnemonic.act.platform.seb.model.v1.SubjectInfoSEB;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.services.common.auth.InvalidCredentialsException;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class SubjectInfoResolver implements Function<UUID, SubjectInfoSEB> {

  private static final Logger LOGGER = Logging.getLogger(SubjectInfoResolver.class);

  private final SubjectSPI subjectResolver;
  private final ServiceAccountSPI credentialsResolver;

  @Inject
  public SubjectInfoResolver(SubjectSPI subjectResolver, ServiceAccountSPI credentialsResolver) {
    this.subjectResolver = subjectResolver;
    this.credentialsResolver = credentialsResolver;
  }

  @Override
  public SubjectInfoSEB apply(UUID id) {
    if (id == null) return null;

    Subject subject = resolveSubject(id);
    if (subject == null) return null;

    return SubjectInfoSEB.builder()
            .setId(subject.getId())
            .setName(subject.getName())
            .build();
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
