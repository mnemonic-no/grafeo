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
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static no.mnemonic.commons.utilities.ObjectUtils.ifNotNull;
import static no.mnemonic.commons.utilities.collections.ListUtils.list;

public class PropertyHelper {

  private static final String OBJECT_VALUE_PROPERTY_NAME = "value";

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
   * Get all object properties. This includes any one legged facts as well as the object value.
   * <p>
   * If there are multiple one legged facts with the same type all of them will be returned.
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
    props.add(new PropertyEntry<>(OBJECT_VALUE_PROPERTY_NAME, objectRecord.getValue()));

    return props;
  }

  /**
   * Get object properties for a specific key (either the object value or any one legged facts).
   * <p>
   * A key of 'value' will be mapped to the object value. Any other key will be mapped to one legged facts of a specific type
   * where key equals the type name. If there are multiple one legged facts with the same type all of them will be returned.
   * <p>
   * Respects the traverseParams for time and retraction status.
   *
   * @param objectRecord   The object record
   * @param traverseParams How the traversal is configured
   * @param propertyKey    The property key
   * @return A list of the object's properties for a specific key
   */
  public List<PropertyEntry<?>> getObjectProperties(ObjectRecord objectRecord, TraverseParams traverseParams, String propertyKey) {
    if (objectRecord == null) throw new IllegalArgumentException("Missing required argument objectRecord!");
    if (traverseParams == null) throw new IllegalArgumentException("Missing required argument traverseParams!");
    if (StringUtils.isBlank(propertyKey)) throw new IllegalArgumentException("Missing required argument propertyKey!");

    // Explicitly handle the 'value' property. Theoretically an object can have one legged facts of type 'value', however,
    // that is an edge case. Why would you do that if you can use the object's value? Ignoring that case to avoid resolving
    // one legged facts which will reduce the number of requests towards ElasticSearch and Cassandra.
    if (Objects.equals(propertyKey, OBJECT_VALUE_PROPERTY_NAME)) {
      return list(new PropertyEntry<>(OBJECT_VALUE_PROPERTY_NAME, objectRecord.getValue()));
    }

    // Otherwise, need to resolve one legged facts to find property. An object usually has not a lot of one legged facts.
    // Because of that, resolving all of them should not cause a large performance hit. Can be optimized later if needed.
    return getOneLeggedFactsAsProperties(objectRecord, traverseParams)
            .stream()
            .filter(p -> Objects.equals(p.getName(), propertyKey))
            .collect(Collectors.toList());
  }

  /**
   * Get the object's one legged facts to be used as part of the object properties.
   * <p>
   * The property key is the name of the fact type and the value is the fact value.
   * Example: name -> name of some report
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
            .map(fact -> new PropertyEntry<>(objectFactTypeResolver.toFactTypeStruct(fact.getTypeID()).getName(),
                    fact.getValue(), fact.getLastSeenTimestamp()))
            .collect(Collectors.toList());
  }

  /**
   * Get all fact properties. This includes meta facts and static fact properties.
   * <p>
   * If there are multiple meta facts with the same type all of them will be returned.
   * <p>
   * Respects the traverseParams for time and retraction status.
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
   * Get a single fact property for a specific key.
   * <p>
   * Keys starting with 'meta/' will be mapped to meta facts of the form 'meta/$type_name'. All other keys will be mapped
   * to static fact properties. See {@link StaticFactProperty} for all supported properties. If there are multiple meta
   * facts with the same type all of them will be returned.
   * <p>
   * Respects the traverseParams for time and retraction status.
   *
   * @param factRecord     Fact record
   * @param traverseParams How the traversal is configured
   * @param propertyKey    The property key
   * @return A list of the fact's properties for a specific key
   */
  public List<PropertyEntry<?>> getFactProperties(FactRecord factRecord, TraverseParams traverseParams, String propertyKey) {
    if (factRecord == null) throw new IllegalArgumentException("Missing required argument factRecord!");
    if (traverseParams == null) throw new IllegalArgumentException("Missing required argument traverseParams!");
    if (StringUtils.isBlank(propertyKey)) throw new IllegalArgumentException("Missing required argument propertyKey!");

    // By convention meta facts are exposed as properties with the "meta/" prefix. A fact usually has not a lot of meta facts.
    // Because of that, resolving all of them should not cause a large performance hit. Can be optimized later if needed.
    if (propertyKey.startsWith("meta/")) {
      return getMetaFactsAsProperties(factRecord, traverseParams)
              .stream()
              .filter(p -> Objects.equals(p.getName(), propertyKey))
              .collect(Collectors.toList());
    }

    // Try to resolve the requested property from the list of static properties.
    StaticFactProperty property = StaticFactProperty.fromKey(propertyKey);
    if (property != null) {
      return getStaticFactProperty(factRecord, property);
    }

    // No matching property found!
    return list();
  }

  /**
   * Get the meta facts to be used as part of a fact properties.
   * <p>
   * The property key is the name of the fact type and the value is the fact value.
   * Example: tlp -> green
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
            .map(record -> new PropertyEntry<>("meta/" + objectFactTypeResolver.toFactTypeStruct(record.getTypeID()).getName(),
                    record.getValue(), record.getLastSeenTimestamp()))
            .collect(Collectors.toList());
  }

  /**
   * Get all static fact properties.
   *
   * @param factRecord Fact record
   * @return A list of the fact's static properties
   */
  List<PropertyEntry<?>> getStaticFactProperties(FactRecord factRecord) {
    return Arrays.stream(StaticFactProperty.values())
            .map(property -> getStaticFactProperty(factRecord, property))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
  }

  private List<PropertyEntry<?>> getStaticFactProperty(FactRecord factRecord, StaticFactProperty property) {
    // Handle all known static fact properties. Return an empty List if the property isn't present.
    switch (property) {
      case accessMode:
        return ifNotNull(factRecord.getAccessMode(),
                mode -> list(new PropertyEntry<>(StaticFactProperty.accessMode.name(), mode.name())),
                list());
      case addedByID:
        return ifNotNull(factRecord.getAddedByID(),
                id -> list(new PropertyEntry<>(StaticFactProperty.addedByID.name(), id.toString())),
                list());
      case addedByName:
        if (factRecord.getAddedByID() == null) return list();
        return ifNotNull(subjectResolver.apply(factRecord.getAddedByID()),
                subject -> list(new PropertyEntry<>(StaticFactProperty.addedByName.name(), subject.getName())),
                list());
      case certainty:
        // Round 'certainty' to two decimal points.
        float certainty = BigDecimal.valueOf(factRecord.getTrust() * factRecord.getConfidence()).setScale(2, RoundingMode.HALF_UP).floatValue();
        return list(new PropertyEntry<>(StaticFactProperty.certainty.name(), certainty));
      case confidence:
        return list(new PropertyEntry<>(StaticFactProperty.confidence.name(), factRecord.getConfidence()));
      case isRetracted:
        return list(new PropertyEntry<>(StaticFactProperty.isRetracted.name(), factRetractionHandler.isRetracted(factRecord)));
      case lastSeenTimestamp:
        return list(new PropertyEntry<>(StaticFactProperty.lastSeenTimestamp.name(), factRecord.getLastSeenTimestamp()));
      case organizationID:
        return ifNotNull(factRecord.getOrganizationID(),
                id -> list(new PropertyEntry<>(StaticFactProperty.organizationID.name(), id.toString())),
                list());
      case organizationName:
        if (factRecord.getOrganizationID() == null) return list();
        return ifNotNull(organizationResolver.apply(factRecord.getOrganizationID()),
                organization -> list(new PropertyEntry<>(StaticFactProperty.organizationName.name(), organization.getName())),
                list());
      case originID:
        return ifNotNull(factRecord.getOriginID(),
                id -> list(new PropertyEntry<>(StaticFactProperty.originID.name(), id.toString())),
                list());
      case originName:
        if (factRecord.getOriginID() == null) return list();
        return ifNotNull(originResolver.apply(factRecord.getOriginID()),
                origin -> list(new PropertyEntry<>(StaticFactProperty.originName.name(), origin.getName())),
                list());
      case timestamp:
        return list(new PropertyEntry<>(StaticFactProperty.timestamp.name(), factRecord.getTimestamp()));
      case trust:
        return list(new PropertyEntry<>(StaticFactProperty.trust.name(), factRecord.getTrust()));
      case value:
        return ifNotNull(factRecord.getValue(),
                value -> list(new PropertyEntry<>(StaticFactProperty.value.name(), value)),
                list());
    }

    // If this exception is thrown it means that one enum value isn't properly handled in the switch above. That's a coding error!
    throw new IllegalArgumentException(String.format("Missing handling of property '%s'!", property));
  }

  /**
   * Helper enum which specifies all properties statically defined on a fact, i.e. no meta facts exposed as properties.
   */
  enum StaticFactProperty {
    accessMode,
    addedByID,
    addedByName,
    certainty,
    confidence,
    isRetracted,
    lastSeenTimestamp,
    organizationID,
    organizationName,
    originID,
    originName,
    timestamp,
    trust,
    value;

    static StaticFactProperty fromKey(String propertyKey) {
      try {
        return StaticFactProperty.valueOf(propertyKey);
      } catch (IllegalArgumentException ignored) {
        return null;
      }
    }
  }
}
