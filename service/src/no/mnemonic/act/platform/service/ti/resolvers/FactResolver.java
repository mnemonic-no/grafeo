package no.mnemonic.act.platform.service.ti.resolvers;

import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.UUID;

public class FactResolver {

  private final ObjectFactDao objectFactDao;

  @Inject
  public FactResolver(ObjectFactDao objectFactDao) {
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
