package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class OrganizationByIdConverterTest {

  @Mock
  private OrganizationResolver organizationResolver;

  private OrganizationByIdConverter converter;

  @Before
  public void setup() {
    initMocks(this);
    converter = new OrganizationByIdConverter(organizationResolver);
  }

  @Test
  public void testConvertOrganization() {
    UUID id = UUID.randomUUID();
    Organization model = Organization.builder().build();

    when(organizationResolver.resolveOrganization(id)).thenReturn(model);

    assertSame(model, converter.apply(id));
    verify(organizationResolver).resolveOrganization(id);
  }

  @Test
  public void testConvertOrganizationNotAvailable() {
    UUID id = UUID.randomUUID();
    Organization model = converter.apply(id);

    assertNotNull(model);
    assertEquals(id, model.getId());
    assertEquals("N/A", model.getName());

    verify(organizationResolver).resolveOrganization(id);
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }
}
