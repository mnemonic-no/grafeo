package no.mnemonic.act.platform.dao.facade.utilities;

import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Function;

/**
 * Wrapper around an {@link Iterator} which converts all elements using a mapping function while
 * iterating the underlying {@link Iterator}. The mapping function should be fast and non-blocking.
 *
 * @param <I> Type of input objects
 * @param <O> Type of output objects
 */
public class MappingIterator<I, O> implements Iterator<O> {

  private final Iterator<I> input;
  private final Function<I, O> mapper;

  /**
   * Construct a new instance.
   *
   * @param input  Wrapped iterator (can be null, defaults to an empty iterator)
   * @param mapper Mapping function to apply during iteration (cannot be null)
   */
  public MappingIterator(Iterator<I> input, Function<I, O> mapper) {
    this.input = ObjectUtils.ifNull(input, Collections.emptyIterator());
    this.mapper = ObjectUtils.notNull(mapper, "'mapper' cannot be null!");
  }

  @Override
  public boolean hasNext() {
    return input.hasNext();
  }

  @Override
  public O next() {
    return mapper.apply(input.next());
  }
}
