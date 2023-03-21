package no.mnemonic.services.grafeo.dao.elastic.result;

import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ScrollingSearchResultTest {

  @Test
  public void testWithEmptyBatch() {
    ScrollingSearchResult<UUID> result = ScrollingSearchResult.<UUID>builder().build();
    assertFalse(result.hasNext());
  }

  @Test
  public void testWithSingleBatch() {
    List<UUID> values = ListUtils.list(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    ScrollingSearchResult<UUID> result = ScrollingSearchResult.<UUID>builder()
            .setInitialBatch(new ScrollingSearchResult.ScrollingBatch<>("TEST_SCROLL_ID", values.iterator(), true))
            .build();
    assertEquals(values, ListUtils.list(result));
  }

  @Test
  public void testWithSingleBatchNoDuplicates() {
    UUID duplicate = UUID.randomUUID();
    List<UUID> values = ListUtils.list(duplicate, duplicate, duplicate);
    ScrollingSearchResult<UUID> result = ScrollingSearchResult.<UUID>builder()
            .setInitialBatch(new ScrollingSearchResult.ScrollingBatch<>("TEST_SCROLL_ID", values.iterator(), true))
            .build();
    assertEquals(ListUtils.list(duplicate), ListUtils.list(result));
  }

  @Test
  public void testWithMultipleBatches() {
    List<UUID> values1 = ListUtils.list(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    List<UUID> values2 = ListUtils.list(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    ScrollingSearchResult<UUID> result = ScrollingSearchResult.<UUID>builder()
            .setInitialBatch(new ScrollingSearchResult.ScrollingBatch<>("TEST_SCROLL_ID", values1.iterator(), false))
            .setFetchNextBatch(s -> new ScrollingSearchResult.ScrollingBatch<>("TEST_SCROLL_ID", values2.iterator(), true))
            .build();
    assertEquals(ListUtils.concatenate(values1, values2), ListUtils.list(result));
  }

  @Test
  public void testWithMultipleBatchesNoDuplicates() {
    UUID duplicate = UUID.randomUUID();
    List<UUID> values1 = ListUtils.list(UUID.randomUUID(), UUID.randomUUID(), duplicate);
    List<UUID> values2 = ListUtils.list(UUID.randomUUID(), duplicate, UUID.randomUUID());
    ScrollingSearchResult<UUID> result = ScrollingSearchResult.<UUID>builder()
            .setInitialBatch(new ScrollingSearchResult.ScrollingBatch<>("TEST_SCROLL_ID", values1.iterator(), false))
            .setFetchNextBatch(s -> new ScrollingSearchResult.ScrollingBatch<>("TEST_SCROLL_ID", values2.iterator(), true))
            .build();
    List<UUID> deduplicated = ListUtils.list(result);
    assertEquals(5, deduplicated.size());
    assertEquals(duplicate, deduplicated.get(2)); // Should keep first occurrence.
  }
}
