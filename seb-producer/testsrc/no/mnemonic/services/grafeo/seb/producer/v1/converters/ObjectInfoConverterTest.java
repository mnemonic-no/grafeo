package no.mnemonic.services.grafeo.seb.producer.v1.converters;

import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.seb.model.v1.ObjectInfoSEB;
import no.mnemonic.services.grafeo.seb.model.v1.ObjectTypeInfoSEB;
import no.mnemonic.services.grafeo.seb.producer.v1.resolvers.ObjectTypeInfoDaoResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ObjectInfoConverterTest {

  @Mock
  private ObjectTypeInfoDaoResolver typeResolver;
  @InjectMocks
  private ObjectInfoConverter converter;

  @Test
  public void testConvertNull() {
    assertNull(converter.apply(null));
  }

  @Test
  public void testConvertEmpty() {
    assertNotNull(converter.apply(new ObjectRecord()));
  }

  @Test
  public void testConvertFull() {
    when(typeResolver.apply(any())).thenReturn(ObjectTypeInfoSEB.builder().build());

    ObjectRecord record = new ObjectRecord()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value");

    ObjectInfoSEB seb = converter.apply(record);
    assertNotNull(seb);
    assertEquals(record.getId(), seb.getId());
    assertNotNull(seb.getType());
    assertEquals(record.getValue(), seb.getValue());

    verify(typeResolver).apply(record.getTypeID());
  }
}
