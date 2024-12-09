package no.mnemonic.services.grafeo.service.implementation.resolvers.request;

import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FactRequestResolverTest {

  @Mock
  private ObjectFactDao objectFactDao;
  @InjectMocks
  private FactRequestResolver resolver;

  @Test
  public void testResolveFactWithNull() {
    assertThrows(ObjectNotFoundException.class, () -> resolver.resolveFact(null));
  }

  @Test
  public void testResolveFactNotFound() {
    assertThrows(ObjectNotFoundException.class, () -> resolver.resolveFact(UUID.randomUUID()));
  }

  @Test
  public void testResolveFactFound() throws Exception {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID());
    when(objectFactDao.getFact(fact.getId())).thenReturn(fact);

    assertSame(fact, resolver.resolveFact(fact.getId()));
    verify(objectFactDao).getFact(fact.getId());
  }
}
