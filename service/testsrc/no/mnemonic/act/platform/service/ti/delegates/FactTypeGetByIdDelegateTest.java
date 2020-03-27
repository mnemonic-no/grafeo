package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.request.v1.GetFactTypeByIdRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.FactTypeConverter;
import no.mnemonic.act.platform.service.ti.resolvers.FactTypeResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactTypeGetByIdDelegateTest {

  @Mock
  private FactTypeResolver factTypeResolver;
  @Mock
  private FactTypeConverter factTypeConverter;
  @Mock
  private TiSecurityContext securityContext;

  private FactTypeGetByIdDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);
    delegate = new FactTypeGetByIdDelegate(
      securityContext,
      factTypeConverter,
      factTypeResolver);
  }

  @Test(expected = AccessDeniedException.class)
  public void testFetchFactTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.viewTypes);
    delegate.handle(new GetFactTypeByIdRequest());
  }

  @Test
  public void testFetchFactType() throws Exception {
    UUID id = UUID.randomUUID();
    FactTypeEntity entity = new FactTypeEntity();

    when(factTypeResolver.fetchExistingFactType(id)).thenReturn(entity);
    delegate.handle(new GetFactTypeByIdRequest().setId(id));
    verify(factTypeConverter).apply(entity);
  }
}
