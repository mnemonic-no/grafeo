package no.mnemonic.act.platform.service.ti.resolvers;

import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.commons.utilities.StringUtils;

import javax.inject.Inject;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Resolver fetching an Origin from the database.
 * <p>
 * If the Origin represents a user the resolver will try to fetch the corresponding user from the access controller
 * and updates the Origin based on the user data stored in the access controller because the access controller holds
 * the authoritative user data.
 */
public class OriginResolver implements Function<UUID, OriginEntity> {

  private static final String NOT_AVAILABLE_NAME = "N/A";

  private final OriginManager originManager;
  private final SubjectResolver subjectResolver;

  @Inject
  public OriginResolver(OriginManager originManager, SubjectResolver subjectResolver) {
    this.originManager = originManager;
    this.subjectResolver = subjectResolver;
  }

  @Override
  public OriginEntity apply(UUID id) {
    // Fetch Origin from database first.
    OriginEntity origin = originManager.getOrigin(id);
    if (origin == null) return null;

    // If Origin is of type 'group' just return it.
    if (origin.getType() == OriginEntity.Type.Group) return origin;

    // Fetch user corresponding to Origin (should have the same id).
    Subject user = subjectResolver.resolveSubject(origin.getId());
    // If no user is found just return Origin.
    if (user == null) return origin;

    // Update fields on Origin based on resolved user and return updated Origin.
    return updateFields(origin, user);
  }

  private OriginEntity updateFields(OriginEntity origin, Subject user) {
    boolean updated = false;

    // The default SubjectResolver implementation will always return an object. If it can't find a subject the name is set
    // to 'N/A'. In this case don't update the Origin's name. Also verify that no other Origin with the user's name exists.
    if (!StringUtils.isBlank(user.getName()) &&
            !Objects.equals(user.getName(), NOT_AVAILABLE_NAME) &&
            !Objects.equals(user.getName(), origin.getName()) &&
            !originWithSameNameExists(origin, user)) {
      origin.setName(user.getName());
      updated = true;
    }

    if (user.getOrganization() != null && !Objects.equals(user.getOrganization().getId(), origin.getOrganizationID())) {
      origin.setOrganizationID(user.getOrganization().getId());
      updated = true;
    }

    // Don't save origin if it hasn't been updated.
    if (!updated) return origin;

    return originManager.saveOrigin(origin);
  }

  private boolean originWithSameNameExists(OriginEntity origin, Subject user) {
    // Check if there exists another Origin with the user's name because there can't exist two Origins with the same name.
    OriginEntity existing = originManager.getOrigin(user.getName());
    return existing != null && !Objects.equals(origin.getId(), existing.getId());
  }
}
