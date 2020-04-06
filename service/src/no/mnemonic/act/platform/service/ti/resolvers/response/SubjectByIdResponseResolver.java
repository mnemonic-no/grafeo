package no.mnemonic.act.platform.service.ti.resolvers.response;

import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class SubjectByIdResponseResolver implements Function<UUID, Subject> {

  private final SubjectResolver subjectResolver;
  private final Map<UUID, Subject> responseCache;

  @Inject
  public SubjectByIdResponseResolver(SubjectResolver subjectResolver, Map<UUID, Subject> responseCache) {
    this.subjectResolver = subjectResolver;
    this.responseCache = responseCache;
  }

  @Override
  public Subject apply(UUID id) {
    if (id == null) return null;
    return responseCache.computeIfAbsent(id, this::resolveUncached);
  }

  private Subject resolveUncached(UUID id) {
    return ObjectUtils.ifNull(subjectResolver.resolveSubject(id), Subject.builder()
            .setId(id)
            .setName("N/A")
            .build()
    );
  }
}
