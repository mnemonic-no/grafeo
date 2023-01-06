package no.mnemonic.act.platform.seb.esengine.v1.converters;

import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.seb.model.v1.*;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FactConverter implements Function<FactSEB, FactDocument> {

  @Override
  public FactDocument apply(FactSEB seb) {
    if (seb == null) return null;

    FactDocument document = new FactDocument()
            .setId(seb.getId())
            .setTypeID(ObjectUtils.ifNotNull(seb.getType(), FactTypeInfoSEB::getId))
            .setValue(seb.getValue())
            .setInReferenceTo(ObjectUtils.ifNotNull(seb.getInReferenceTo(), FactInfoSEB::getId))
            .setOrganizationID(ObjectUtils.ifNotNull(seb.getOrganization(), OrganizationInfoSEB::getId))
            .setOriginID(ObjectUtils.ifNotNull(seb.getOrigin(), OriginInfoSEB::getId))
            .setAddedByID(ObjectUtils.ifNotNull(seb.getAddedBy(), SubjectInfoSEB::getId))
            .setLastSeenByID(ObjectUtils.ifNotNull(seb.getLastSeenBy(), SubjectInfoSEB::getId))
            .setAccessMode(ObjectUtils.ifNotNull(seb.getAccessMode(), m -> FactDocument.AccessMode.valueOf(m.name())))
            .setConfidence(seb.getConfidence())
            .setTrust(seb.getTrust())
            .setTimestamp(seb.getTimestamp())
            .setLastSeenTimestamp(seb.getLastSeenTimestamp())
            .setAcl(convertAcl(seb.getAcl()))
            .setFlags(SetUtils.set(seb.getFlags(), flag -> FactDocument.Flag.valueOf(flag.name())));

    if (seb.getSourceObject() != null) {
      ObjectDocument.Direction direction = seb.isBidirectionalBinding() ? ObjectDocument.Direction.BiDirectional : ObjectDocument.Direction.FactIsDestination;
      document.addObject(convertObject(seb.getSourceObject(), direction));
    }

    if (seb.getDestinationObject() != null) {
      ObjectDocument.Direction direction = seb.isBidirectionalBinding() ? ObjectDocument.Direction.BiDirectional : ObjectDocument.Direction.FactIsSource;
      document.addObject(convertObject(seb.getDestinationObject(), direction));
    }

    return document;
  }

  private Set<UUID> convertAcl(Set<AclEntrySEB> acl) {
    if (CollectionUtils.isEmpty(acl)) return Collections.emptySet();
    return acl.stream()
            .filter(Objects::nonNull)
            .map(AclEntrySEB::getSubject)
            .filter(Objects::nonNull)
            .map(SubjectInfoSEB::getId)
            .collect(Collectors.toSet());
  }

  private ObjectDocument convertObject(ObjectInfoSEB seb, ObjectDocument.Direction direction) {
    return new ObjectDocument()
            .setId(seb.getId())
            .setTypeID(ObjectUtils.ifNotNull(seb.getType(), ObjectTypeInfoSEB::getId))
            .setValue(seb.getValue())
            .setDirection(direction);
  }
}
