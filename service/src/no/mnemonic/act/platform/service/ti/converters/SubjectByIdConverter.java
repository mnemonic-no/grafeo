package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class SubjectByIdConverter implements Function<UUID, Subject> {

  private final SubjectResolver subjectResolver;

  @Inject
  public SubjectByIdConverter(SubjectResolver subjectResolver) {
    this.subjectResolver = subjectResolver;
  }

  @Override
  public Subject apply(UUID id) {
    if (id == null) return null;
    return ObjectUtils.ifNull(subjectResolver.resolveSubject(id), Subject.builder()
            .setId(id)
            .setName("N/A")
            .build()
    );
  }
}
