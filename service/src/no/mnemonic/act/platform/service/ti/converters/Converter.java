package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.commons.utilities.StringUtils;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Convert an object of type A to an object of type B.
 *
 * @param <A> Source type
 * @param <B> Target type
 */
public interface Converter<A, B> extends Function<A, B> {
  /**
   * Get type of source object.
   *
   * @return Source type
   */
  Class<A> getSourceType();

  /**
   * Get Type of target object.
   *
   * @return Target type
   */
  Class<B> getTargetType();

  /**
   * Perform conversion from source object to target object.
   *
   * @param source Source object
   * @return Target object
   */
  @Override
  B apply(A source);

  /**
   * Filter out all entries from a Set which contain a UUID.
   *
   * @param field Set of Strings
   * @return Set of UUIDs
   */
  default Set<UUID> onlyUUID(Set<String> field) {
    if (field == null) return null;
    return field.stream()
            .filter(StringUtils::isUUID)
            .map(UUID::fromString)
            .collect(Collectors.toSet());
  }

  /**
   * Filter out all entries from a Set which do not contain a UUID.
   *
   * @param field Set of Strings
   * @return Set of Strings
   */
  default Set<String> noneUUID(Set<String> field) {
    if (field == null) return null;
    return field.stream()
            .filter(value -> !StringUtils.isUUID(value))
            .collect(Collectors.toSet());
  }
}
