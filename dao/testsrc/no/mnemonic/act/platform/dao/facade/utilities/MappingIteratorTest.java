package no.mnemonic.act.platform.dao.facade.utilities;

import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MappingIteratorTest {

  @Test(expected = RuntimeException.class)
  public void testInitializeWithoutMapper() {
    new MappingIterator<>(ListUtils.list(1, 2, 3).iterator(), null);
  }

  @Test
  public void testInitializeWithoutIterator() {
    Iterator<String> iterator = new MappingIterator<>(null, String::valueOf);
    assertFalse(iterator.hasNext());
  }

  @Test
  public void testMappingDuringIteration() {
    Iterator<String> iterator = new MappingIterator<>(ListUtils.list(1, 2, 3).iterator(), String::valueOf);
    assertEquals(ListUtils.list("1", "2", "3"), ListUtils.list(iterator));
  }
}
