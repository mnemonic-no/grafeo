package no.mnemonic.act.platform.service.ti.converters.request;

import no.mnemonic.act.platform.api.request.v1.SearchFactRequest;
import no.mnemonic.act.platform.api.request.v1.TimeFieldSearchRequest;
import no.mnemonic.act.platform.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.criteria.IndexSelectCriteria;
import org.junit.Test;

import java.util.UUID;

import static no.mnemonic.act.platform.service.ti.converters.request.RequestConverterUtils.handleTimeFieldSearchRequest;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RequestConverterUtilsTest {

  @Test
  public void testHandleTimeFieldSearchRequest() {
    SearchFactRequest request = new SearchFactRequest()
            .setStartTimestamp(123456789L)
            .setEndTimestamp(987654321L)
            .addTimeFieldStrategy(TimeFieldSearchRequest.TimeFieldStrategy.timestamp)
            .setTimeMatchStrategy(TimeFieldSearchRequest.TimeMatchStrategy.all);

    FactSearchCriteria criteria = handleTimeFieldSearchRequest(criteriaBuilder(), request).build();
    assertEquals(request.getStartTimestamp(), criteria.getStartTimestamp());
    assertEquals(request.getEndTimestamp(), criteria.getEndTimestamp());
    assertEquals(set(request.getTimeFieldStrategy(), Enum::name), set(criteria.getTimeFieldStrategy(), Enum::name));
    assertEquals(request.getTimeMatchStrategy().name(), criteria.getTimeMatchStrategy().name());
  }

  @Test
  public void testHandleTimeFieldSearchRequestEmptyRequest() {
    FactSearchCriteria criteria = handleTimeFieldSearchRequest(criteriaBuilder(), new SearchFactRequest()).build();
    assertNull(criteria.getStartTimestamp());
    assertNull(criteria.getEndTimestamp());
    assertEquals(set(FactSearchCriteria.TimeFieldStrategy.lastSeenTimestamp), criteria.getTimeFieldStrategy());
    assertEquals(FactSearchCriteria.MatchStrategy.any, criteria.getTimeMatchStrategy());
  }

  private FactSearchCriteria.Builder criteriaBuilder() {
    return FactSearchCriteria.builder()
            .setAccessControlCriteria(AccessControlCriteria.builder()
                    .addCurrentUserIdentity(UUID.randomUUID())
                    .addAvailableOrganizationID(UUID.randomUUID())
                    .build())
            .setIndexSelectCriteria(IndexSelectCriteria.builder().build());
  }
}
