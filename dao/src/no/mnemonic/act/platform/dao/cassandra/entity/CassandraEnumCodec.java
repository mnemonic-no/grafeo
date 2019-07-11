package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.oss.driver.api.core.ProtocolVersion;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.TypeCodecs;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.nio.ByteBuffer;
import java.util.Map;

public class CassandraEnumCodec<E extends Enum<E> & CassandraEnum> implements TypeCodec<E> {

  private static final TypeCodec<Integer> innerCodec = TypeCodecs.INT;

  private final Class<E> enumClass;
  private final Map<Integer, E> enumValueMap;

  public CassandraEnumCodec(Class<E> enumClass, Map<Integer, E> enumValueMap) {
    this.enumClass = ObjectUtils.notNull(enumClass, "'enumClass' cannot be null!");
    this.enumValueMap = ObjectUtils.notNull(enumValueMap, "'enumValueMap' cannot be null!");
  }

  @Override
  public GenericType<E> getJavaType() {
    return GenericType.of(enumClass);
  }

  @Override
  public DataType getCqlType() {
    return DataTypes.INT;
  }

  @Override
  public ByteBuffer encode(E value, ProtocolVersion protocolVersion) {
    return innerCodec.encode(value != null ? value.value() : null, protocolVersion);
  }

  @Override
  public E decode(ByteBuffer bytes, ProtocolVersion protocolVersion) {
    return ObjectUtils.ifNotNull(innerCodec.decode(bytes, protocolVersion), enumValueMap::get);
  }

  @Override
  public String format(E value) {
    return innerCodec.format(value != null ? value.value() : null);
  }

  @Override
  public E parse(String value) {
    return ObjectUtils.ifNotNull(innerCodec.parse(value), enumValueMap::get);
  }
}
