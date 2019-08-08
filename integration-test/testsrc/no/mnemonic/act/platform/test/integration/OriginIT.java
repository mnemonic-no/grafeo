package no.mnemonic.act.platform.test.integration;

import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import org.junit.Test;

import java.util.UUID;

public class OriginIT extends AbstractIT {

  @Test
  public void testFetchOrigin() throws Exception {
    // Create an Origin in the database ...
    OriginEntity entity = createOrigin();

    // ... and check that it can be received via the REST API.
    fetchAndAssertSingle("/v1/origin/uuid/" + entity.getId(), entity.getId());
  }

  private OriginEntity createOrigin() {
    OriginEntity entity = new OriginEntity()
            .setId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .setName("origin");
    return getOriginManager().saveOrigin(entity);
  }

}
