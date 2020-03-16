package no.mnemonic.act.platform.service.ti.helpers;

import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactCommentRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.commons.utilities.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;

public class FactHelper {

  /**
   * Add an additional comment to a FactRecord.
   *
   * @param fact    Fact the comment belongs to
   * @param comment Comment to add
   * @return Fact with added comment
   */
  public static FactRecord withComment(FactRecord fact, String comment) {
    if (fact == null) return null;
    if (StringUtils.isBlank(comment)) return fact;

    return fact.addComment(new FactCommentRecord()
      .setId(UUID.randomUUID())
      .setOriginID(fact.getOriginID())
      .setComment(comment)
      .setTimestamp(System.currentTimeMillis()));
  }

  /**
   * Add additional ACL entries to a FactRecord. It will omit any entries already in the Fact's ACL
   * and makes sure that the current user is in the ACL with AccessMode 'Explicit'.
   *
   * @param fact Fact the ACL belongs to
   * @param currentUserId User id of the current user
   * @param acl  List of Subject IDs
   * @return Fact with updated ACL
   */
  public static FactRecord withAcl(FactRecord fact, UUID currentUserId, List<UUID> acl) {
    if (fact == null) return null;
    // It doesn't make sense to have an ACL when Fact is public.
    if (fact.getAccessMode() == FactRecord.AccessMode.Public) return fact;

    // Fetch any existing entries ...
    Set<UUID> existingAcl = set(fact.getAcl(), FactAclEntryRecord::getSubjectID);
    // ... and make sure not to add any duplicates.
    Set<UUID> subjectsToAdd = set(acl)
      .stream()
      .filter(subject -> !existingAcl.contains(subject))
      .collect(Collectors.toSet());

    if (fact.getAccessMode() == FactRecord.AccessMode.Explicit && !existingAcl.contains(currentUserId)) {
      // Make sure that current user is in the ACL with 'Explicit' AccessMode.
      // With 'RoleBased' AccessMode current user has access to Fact via the Organization.
      subjectsToAdd.add(currentUserId);
    }

    // Add all new ACL entries to Fact.
    for (UUID subject : subjectsToAdd) {
      fact.addAclEntry(new FactAclEntryRecord()
        .setId(UUID.randomUUID())
        .setOriginID(fact.getOriginID())
        .setSubjectID(subject)
        .setTimestamp(System.currentTimeMillis())
      );
    }

    return fact;
  }
}
