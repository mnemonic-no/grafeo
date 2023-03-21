package no.mnemonic.services.grafeo.seb.producer.v1.resolvers;

import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.seb.model.v1.FactInfoSEB;
import no.mnemonic.services.grafeo.seb.model.v1.FactTypeInfoSEB;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactInfoDaoResolverTest {

  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private FactTypeInfoDaoResolver typeResolver;

  private FactInfoDaoResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);
    resolver = new FactInfoDaoResolver(objectFactDao, typeResolver);
  }

  @Test
  public void testResolveNull() {
    assertNull(resolver.apply(null));
  }

  @Test
  public void testResolveNoFactFound() {
    UUID id = UUID.randomUUID();

    FactInfoSEB seb = resolver.apply(id);
    assertNotNull(seb);
    assertEquals(id, seb.getId());

    verify(objectFactDao).getFact(id);
  }

  @Test
  public void testResolveFactFound() {
    FactRecord record = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value");
    when(objectFactDao.getFact(any())).thenReturn(record);
    when(typeResolver.apply(any())).thenReturn(FactTypeInfoSEB.builder().build());

    FactInfoSEB seb = resolver.apply(record.getId());
    assertNotNull(seb);
    assertEquals(record.getId(), seb.getId());
    assertNotNull(seb.getType());
    assertEquals(record.getValue(), seb.getValue());

    verify(objectFactDao).getFact(record.getId());
    verify(typeResolver).apply(record.getTypeID());
  }
}
