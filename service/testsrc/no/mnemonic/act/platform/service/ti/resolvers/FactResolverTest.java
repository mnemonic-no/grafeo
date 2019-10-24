package no.mnemonic.act.platform.service.ti.resolvers;

import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactResolverTest {

  @Mock
  private ObjectFactDao objectFactDao;

  private FactResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);
    resolver = new FactResolver(objectFactDao);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testResolveFactWithNull() throws Exception {
    resolver.resolveFact(null);
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testResolveFactNotFound() throws Exception {
    resolver.resolveFact(UUID.randomUUID());
  }

  @Test
  public void testResolveFactFound() throws Exception {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID());
    when(objectFactDao.getFact(fact.getId())).thenReturn(fact);

    assertSame(fact, resolver.resolveFact(fact.getId()));
    verify(objectFactDao).getFact(fact.getId());
  }
}
