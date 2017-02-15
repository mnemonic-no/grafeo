package no.mnemonic.act.platform.service.ti.converters;

import java.util.function.Function;

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
}
