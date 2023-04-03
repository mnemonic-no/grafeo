package no.mnemonic.services.grafeo.service.implementation.resolvers.request;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;

import javax.inject.Inject;
import java.util.UUID;

public class FactRequestResolver {

  private final ObjectFactDao objectFactDao;

  @Inject
  public FactRequestResolver(ObjectFactDao objectFactDao) {
    this.objectFactDao = objectFactDao;
  }

  /**
   * Fetch an existing Fact by ID.
   *
   * @param id UUID of Fact
   * @return Existing Fact
   * @throws ObjectNotFoundException Thrown if Fact cannot be found
   */
  public FactRecord resolveFact(UUID id) throws ObjectNotFoundException {
    FactRecord record = objectFactDao.getFact(id);
    if (record == null) {
      throw new ObjectNotFoundException(String.format("Fact with id = %s does not exist.", id),
              "fact.not.exist", "id", ObjectUtils.ifNotNull(id, Object::toString, "NULL"));
    }
    return record;
  }
}
