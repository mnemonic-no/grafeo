package no.mnemonic.act.platform.seb.producer.v1.resolvers;

import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.seb.model.v1.SubjectInfoSEB;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class SubjectInfoResolver implements Function<UUID, SubjectInfoSEB> {

  private final SubjectResolver subjectResolver;

  @Inject
  public SubjectInfoResolver(SubjectResolver subjectResolver) {
    this.subjectResolver = subjectResolver;
  }

  @Override
  public SubjectInfoSEB apply(UUID id) {
    if (id == null) return null;

    Subject subject = subjectResolver.resolveSubject(id);
    if (subject == null) return null;

    return SubjectInfoSEB.builder()
            .setId(subject.getId())
            .setName(subject.getName())
            .build();
  }
}
