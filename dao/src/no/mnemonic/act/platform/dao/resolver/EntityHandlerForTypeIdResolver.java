package no.mnemonic.act.platform.dao.resolver;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.entity.cassandra.FactTypeEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import no.mnemonic.act.platform.entity.handlers.EntityHandler;
import no.mnemonic.act.platform.entity.handlers.EntityHandlerFactory;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

/**
 * Class resolving an EntityHandler given the id of an ObjectType or FactType.
 */
public class EntityHandlerForTypeIdResolver implements Function<UUID, EntityHandler> {

  private final ObjectManager objectManager;
  private final FactManager factManager;
  private final EntityHandlerFactory factory;

  @Inject
  public EntityHandlerForTypeIdResolver(ObjectManager objectManager, FactManager factManager, EntityHandlerFactory factory) {
    this.objectManager = objectManager;
    this.factManager = factManager;
    this.factory = factory;
  }

  @Override
  public EntityHandler apply(UUID typeID) {
    ObjectTypeEntity objectType = objectManager.getObjectType(typeID);
    if (objectType != null) {
      return factory.get(objectType.getEntityHandler(), objectType.getEntityHandlerParameter());
    }

    FactTypeEntity factType = factManager.getFactType(typeID);
    if (factType != null) {
      return factory.get(factType.getEntityHandler(), factType.getEntityHandlerParameter());
    }

    throw new IllegalArgumentException(String.format("Could not find ObjectType nor FactType for id = %s.", typeID));
  }

}
