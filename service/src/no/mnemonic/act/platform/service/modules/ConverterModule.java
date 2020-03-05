package no.mnemonic.act.platform.service.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

/**
 * Module which configures the converters used by the service implementation.
 */
class ConverterModule extends AbstractModule {

  @Provides
  Function<UUID, Subject> provideSubjectByIdConverter(SubjectResolver resolver) {
    return id -> ObjectUtils.ifNotNull(id, resolver::resolveSubject);
  }

  @Provides
  Function<UUID, Organization> provideOrganizationByIdConverter(OrganizationResolver resolver) {
    return id -> ObjectUtils.ifNotNull(id, resolver::resolveOrganization);
  }

  @Provides
  Function<UUID, FactTypeEntity> provideFactTypeEntityResolver(FactManager manager) {
    return manager::getFactType;
  }

  @Provides
  Function<UUID, Collection<ObjectStatisticsContainer.FactStatistic>> provideFactStatisticsResolver() {
    // Don't include statistics in the default ObjectConverter, e.g. when including Objects as part of Facts.
    // When statistics should be included in responses create a new instance of the ObjectConverter instead.
    return id -> Collections.emptyList();
  }
}
