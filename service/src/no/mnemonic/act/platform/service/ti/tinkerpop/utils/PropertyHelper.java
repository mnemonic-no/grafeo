package no.mnemonic.act.platform.service.ti.tinkerpop.utils;

import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.FactRetractionHandler;
import no.mnemonic.act.platform.service.ti.resolvers.response.OrganizationByIdResponseResolver;
import no.mnemonic.act.platform.service.ti.resolvers.response.OriginByIdResponseResolver;
import no.mnemonic.act.platform.service.ti.resolvers.response.SubjectByIdResponseResolver;
import no.mnemonic.act.platform.service.ti.tinkerpop.TraverseParams;
import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import static no.mnemonic.commons.utilities.ObjectUtils.ifNotNull;
import static no.mnemonic.commons.utilities.ObjectUtils.ifNotNullDo;
import static no.mnemonic.commons.utilities.collections.ListUtils.addToList;
import static no.mnemonic.commons.utilities.collections.ListUtils.list;

public class PropertyHelper {

  private final ObjectFactDao objectFactDao;
  private final TiSecurityContext securityContext;
  private final ObjectFactTypeResolver objectFactTypeResolver;
  private final FactRetractionHandler factRetractionHandler;
  private final SubjectByIdResponseResolver subjectResolver;
  private final OrganizationByIdResponseResolver organizationResolver;
  private final OriginByIdResponseResolver originResolver;

  @Inject
  public PropertyHelper(FactRetractionHandler factRetractionHandler,
                        ObjectFactDao objectFactDao,
                        ObjectFactTypeResolver objectFactTypeResolver,
                        TiSecurityContext securityContext,
                        SubjectByIdResponseResolver subjectResolver,
                        OrganizationByIdResponseResolver organizationResolver,
                        OriginByIdResponseResolver originResolver) {
    this.objectFactDao = objectFactDao;
    this.securityContext = securityContext;
    this.objectFactTypeResolver = objectFactTypeResolver;
    this.factRetractionHandler = factRetractionHandler;
    this.subjectResolver = subjectResolver;
    this.organizationResolver = organizationResolver;
    this.originResolver = originResolver;
  }

  /**
   * Get all object properties
   * This includes any one legged facts as well as the object value.
   * <p>
   * An object may have more than one property with the same name,
   * e.g multiple names or categories. See http://tinkerpop.apache.org/docs/current/reference/#vertex-properties
   * <p>
   * Respects the traverseParams for time and retraction status.
   *
   * @param objectRecord   The object record
   * @param traverseParams How the traversal is configured
   * @return A list of the object's properties
   */
  public List<PropertyEntry<?>> getObjectProperties(ObjectRecord objectRecord, TraverseParams traverseParams) {
    if (objectRecord == null) throw new IllegalArgumentException("Missing required argument objectRecord!");
    if (traverseParams == null) throw new IllegalArgumentException("Missing required argument traverseParams!");

    List<PropertyEntry<?>> props = getOneLeggedFactsAsProperties(objectRecord, traverseParams);
    props.add(new PropertyEntry<>("value", objectRecord.getValue()));

    return props;
  }

  /**
   * Get the object's one-legged facts to be used as part of the object's properties.
   * Note that the properties is a list, not a set. An object may have more than one property with the same name,
   * e.g multiple names or categories. See http://tinkerpop.apache.org/docs/current/reference/#vertex-properties
   * <p>
   * Respects the traverseParams for time and retraction status.
   *
   * <p>
   * The property key is the fact name and the value is the fact value.
   * Example: name - Name of some report
   *
   * @param objectRecord   The object record
   * @param traverseParams How the traversal is configured
   * @return A list of the object's one legged facts in the form of fact type name to fact value
   */
  List<PropertyEntry<?>> getOneLeggedFactsAsProperties(ObjectRecord objectRecord, TraverseParams traverseParams) {

    ResultContainer<FactRecord> facts = objectFactDao.searchFacts(
            traverseParams.getBaseSearchCriteria()
                    .toBuilder()
                    .addObjectID(objectRecord.getId())
                    .setFactBinding(FactSearchCriteria.FactBinding.oneLegged)
                    .build());

    return facts.stream()
            .filter(securityContext::hasReadPermission)
            .filter(record -> traverseParams.isIncludeRetracted() || !factRetractionHandler.isRetracted(record))
            .map(fact -> new PropertyEntry<>(objectFactTypeResolver.toFactTypeStruct(fact.getTypeID()).getName(), fact.getValue()))
            .collect(Collectors.toList());
  }

  /**
   * Get all fact properties
   * This includes meta facts and static fact properties
   * <p>
   * A fact may have only have one property with the same name.
   * See http://tinkerpop.apache.org/docs/current/reference/#vertex-properties
   *
   * @param factRecord     Fact record
   * @param traverseParams How the traversal is configured
   * @return A list of the fact's properties
   */
  public List<PropertyEntry<?>> getFactProperties(FactRecord factRecord, TraverseParams traverseParams) {
    if (factRecord == null) throw new IllegalArgumentException("Missing required argument factRecord!");
    if (traverseParams == null) throw new IllegalArgumentException("Missing required argument traverseParams!");

    return ListUtils.concatenate(
            getMetaFactsAsProperties(factRecord, traverseParams),
            getStaticFactProperties(factRecord));
  }

  /**
   * Get the meta facts to be used as part of a fact's properties.
   * <p>
   * Respects the traverseParams for time and retraction status.
   *
   * <p>
   * The property key is the fact name and the value is the fact value.
   * Example: tlp - green
   *
   * @param factRecord     Fact record
   * @param traverseParams How the traversal is configured
   * @return A list of the fact's meta facts in the form of fact type name to fact value
   */
  List<PropertyEntry<?>> getMetaFactsAsProperties(FactRecord factRecord, TraverseParams traverseParams) {
    ResultContainer<FactRecord> facts = objectFactDao.searchFacts(
            traverseParams.getBaseSearchCriteria()
                    .toBuilder()
                    .addInReferenceTo(factRecord.getId())
                    .setFactBinding(FactSearchCriteria.FactBinding.meta)
                    .build());

    return facts.stream()
            .filter(securityContext::hasReadPermission)
            .filter(record -> traverseParams.isIncludeRetracted() || !factRetractionHandler.isRetracted(record))
            .map(record -> new PropertyEntry<>("meta/" +
                    objectFactTypeResolver.toFactTypeStruct(record.getTypeID()).getName(), record.getValue()))
            .collect(Collectors.toList());
  }

  /**
   * Gets static fact properties
   * <p>
   * Resolves subjectName, organizationName and originName as properties.
   *
   * @param factRecord Fact record
   * @return A list of the fact's static properties
   */
  List<PropertyEntry<?>> getStaticFactProperties(FactRecord factRecord) {
    // Round 'certainty' to two decimal points.
    float certainty = BigDecimal.valueOf(factRecord.getTrust() * factRecord.getConfidence()).setScale(2, RoundingMode.HALF_UP).floatValue();

    List<PropertyEntry<?>> props = list();

    addToList(props, new PropertyEntry<>("value", factRecord.getValue()));
    addToList(props, ifNotNull(factRecord.getOrganizationID(), o -> new PropertyEntry<>("organizationID", o.toString())));
    addToList(props, ifNotNull(factRecord.getOriginID(), o -> new PropertyEntry<>("originID", o.toString())));
    addToList(props, ifNotNull(factRecord.getAddedByID(), o -> new PropertyEntry<>("addedByID", o.toString())));
    addToList(props, ifNotNull(factRecord.getAccessMode(), e -> new PropertyEntry<>("accessMode", e.name())));
    addToList(props, new PropertyEntry<>("timestamp", factRecord.getTimestamp()));
    addToList(props, new PropertyEntry<>("lastSeenTimestamp", factRecord.getLastSeenTimestamp()));
    addToList(props, new PropertyEntry<>("trust", factRecord.getTrust()));
    addToList(props, new PropertyEntry<>("confidence", factRecord.getConfidence()));
    addToList(props, new PropertyEntry<>("certainty", certainty));

    addToList(props, new PropertyEntry<>("isRetracted", factRetractionHandler.isRetracted(factRecord)));
    ifNotNullDo(factRecord.getAddedByID(), id -> addToList(props, ifNotNull(subjectResolver.apply(id), s -> new PropertyEntry<>("addedByName", s.getName()))));
    ifNotNullDo(factRecord.getOrganizationID(), id -> addToList(props, ifNotNull(organizationResolver.apply(id), o -> new PropertyEntry<>("organizationName", o.getName()))));
    ifNotNullDo(factRecord.getOriginID(), id -> addToList(props, ifNotNull(originResolver.apply(id), o -> new PropertyEntry<>("originName", o.getName()))));

    return props;
  }
}
