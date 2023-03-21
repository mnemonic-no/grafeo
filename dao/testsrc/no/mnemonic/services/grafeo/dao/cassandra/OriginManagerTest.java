package no.mnemonic.services.grafeo.dao.cassandra;

import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class OriginManagerTest extends AbstractManagerTest {

  @Test
  public void testSaveAndGetOriginById() {
    OriginEntity entity = createAndSaveOrigin();
    assertOrigin(entity, getOriginManager().getOrigin(entity.getId()));
  }

  @Test
  public void testGetOriginWithUnknownIdReturnsNull() {
    assertNull(getOriginManager().getOrigin((UUID) null));
    assertNull(getOriginManager().getOrigin(UUID.randomUUID()));
  }

  @Test
  public void testGetOriginByIdTwiceReturnsSameInstance() {
    OriginEntity entity = createAndSaveOrigin();
    OriginEntity origin1 = getOriginManager().getOrigin(entity.getId());
    OriginEntity origin2 = getOriginManager().getOrigin(entity.getId());
    assertSame(origin1, origin2);
  }

  @Test
  public void testSaveOriginTwiceInvalidatesIdCache() {
    OriginEntity entity = createOrigin();
    getOriginManager().saveOrigin(entity);
    OriginEntity origin1 = getOriginManager().getOrigin(entity.getId());
    getOriginManager().saveOrigin(entity);
    OriginEntity origin2 = getOriginManager().getOrigin(entity.getId());
    assertNotSame(origin1, origin2);
  }

  @Test
  public void testSaveAndGetOriginByName() {
    OriginEntity entity = createAndSaveOrigin();
    assertOrigin(entity, getOriginManager().getOrigin(entity.getName()));
  }

  @Test
  public void testGetOriginWithUnknownNameReturnsNull() {
    assertNull(getOriginManager().getOrigin((String) null));
    assertNull(getOriginManager().getOrigin(""));
    assertNull(getOriginManager().getOrigin("Unknown"));
  }

  @Test
  public void testGetOriginByNameTwiceReturnsSameInstance() {
    OriginEntity entity = createAndSaveOrigin();
    OriginEntity origin1 = getOriginManager().getOrigin(entity.getName());
    OriginEntity origin2 = getOriginManager().getOrigin(entity.getName());
    assertSame(origin1, origin2);
  }

  @Test
  public void testSaveOriginTwiceInvalidatesNameCache() {
    OriginEntity entity = createOrigin();
    getOriginManager().saveOrigin(entity);
    OriginEntity origin1 = getOriginManager().getOrigin(entity.getName());
    getOriginManager().saveOrigin(entity);
    OriginEntity origin2 = getOriginManager().getOrigin(entity.getName());
    assertNotSame(origin1, origin2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSaveOriginWithSameNameThrowsException() {
    getOriginManager().saveOrigin(createOrigin("origin"));
    getOriginManager().saveOrigin(createOrigin("origin"));
  }

  @Test
  public void testSaveOriginReturnsSameEntity() {
    OriginEntity entity = createOrigin();
    assertSame(entity, getOriginManager().saveOrigin(entity));
  }

  @Test
  public void testSaveOriginReturnsNullOnNullInput() {
    assertNull(getOriginManager().saveOrigin(null));
  }

  @Test
  public void testFetchOrigins() {
    List<OriginEntity> expected = createAndSaveOrigins(3);
    List<OriginEntity> actual = getOriginManager().fetchOrigins();

    expected.sort(Comparator.comparing(OriginEntity::getId));
    actual.sort(Comparator.comparing(OriginEntity::getId));

    assertOrigins(expected, actual);
  }

  private OriginEntity createOrigin() {
    return createOrigin("origin");
  }

  private OriginEntity createOrigin(String name) {
    return new OriginEntity()
            .setId(UUID.randomUUID())
            .setNamespaceID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setName(name)
            .setDescription("description")
            .setTrust(0.1f)
            .setType(OriginEntity.Type.User)
            .addFlag(OriginEntity.Flag.Deleted);
  }

  private OriginEntity createAndSaveOrigin() {
    return createAndSaveOrigins(1).get(0);
  }

  private List<OriginEntity> createAndSaveOrigins(int numberOfEntities) {
    List<OriginEntity> entities = new ArrayList<>();

    for (int i = 0; i < numberOfEntities; i++) {
      entities.add(getOriginManager().saveOrigin(createOrigin("origin" + i)));
    }

    return entities;
  }

  private void assertOrigin(OriginEntity expected, OriginEntity actual) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getNamespaceID(), actual.getNamespaceID());
    assertEquals(expected.getOrganizationID(), actual.getOrganizationID());
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getDescription(), actual.getDescription());
    assertEquals(expected.getTrust(), actual.getTrust(), 0);
    assertEquals(expected.getType(), actual.getType());
    assertEquals(expected.getFlags(), actual.getFlags());
  }

  private void assertOrigins(List<OriginEntity> expected, List<OriginEntity> actual) {
    assertEquals(expected.size(), actual.size());
    for (int i = 0; i < expected.size(); i++) {
      assertOrigin(expected.get(i), actual.get(i));
    }
  }

}
