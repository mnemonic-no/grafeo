package no.mnemonic.act.platform.service.ti.tinkerpop.utils;

import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.FactRetractionHandler;
import no.mnemonic.act.platform.service.ti.tinkerpop.TraverseParams;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PropertyHelper {

  private final ObjectFactDao objectFactDao;
  private final TiSecurityContext securityContext;
  private final ObjectFactTypeResolver objectFactTypeResolver;
  private final FactRetractionHandler factRetractionHandler;

  @Inject
  public PropertyHelper(FactRetractionHandler factRetractionHandler,
                        ObjectFactDao objectFactDao,
                        ObjectFactTypeResolver objectFactTypeResolver,
                        TiSecurityContext securityContext) {
    this.objectFactDao = objectFactDao;
    this.securityContext = securityContext;
    this.objectFactTypeResolver = objectFactTypeResolver;
    this.factRetractionHandler = factRetractionHandler;
  }

  /**
   * Get the object's one-legged facts to be used as part of the object's properties.
   * Note that the properties is a list, not a set. An object may have more than one property with the same name,
   * e.g multiple names or categories. See http://tinkerpop.apache.org/docs/current/reference/#vertex-properties
   *
   * Respects the traverseParams for time and retraction status.
   *
   * <p>
   * The property key is the fact name and the value is the fact value.
   * Example: name, Name of some report
   *
   * @param objectId The object identifier
   * @param traverseParams How the traversal is configured
   * @return A map of the object's one legged facts in the form of fact type name to fact value
   */
  public List<PropertyEntry<String>> getOneLeggedFactsAsProperties(UUID objectId, TraverseParams traverseParams) {
    if (objectId == null) throw new IllegalArgumentException("Missing required argument objectId!");
    if (traverseParams == null) throw new IllegalArgumentException("Missing required argument traverseParams!");

    ResultContainer<FactRecord> facts = objectFactDao.searchFacts(FactSearchCriteria.builder()
            .addObjectID(objectId)
            .setFactBinding(FactSearchCriteria.FactBinding.oneLegged)
            .setStartTimestamp(traverseParams.getAfterTimestamp())
            .setEndTimestamp(traverseParams.getBeforeTimestamp())
            .setCurrentUserID(securityContext.getCurrentUserID())
            .setAvailableOrganizationID(securityContext.getAvailableOrganizationID())
            .build());

    return facts
            .stream()
            .filter(securityContext::hasReadPermission)
            .filter(record -> traverseParams.isIncludeRetracted() || !factRetractionHandler.isRetracted(record))
            .map(fact -> new PropertyEntry<>(objectFactTypeResolver.toFactTypeStruct(fact.getTypeID()).getName(), fact.getValue()))
            .collect(Collectors.toList());
  }
}
