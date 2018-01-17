package no.mnemonic.act.platform.dao.cassandra.entity;

import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.exceptions.InvalidTypeException;

import java.nio.ByteBuffer;
import java.util.Map;

public class CassandraEnumCodec<E extends Enum<E> & CassandraEnum> extends TypeCodec<E> {

  private final Map<Integer, E> enumValueMap;
  private final TypeCodec<Integer> innerCodec;

  public CassandraEnumCodec(Class<E> enumClass, Map<Integer, E> enumValueMap) {
    this(TypeCodec.cint(), enumClass, enumValueMap);
  }

  public CassandraEnumCodec(TypeCodec<Integer> innerCodec, Class<E> enumClass, Map<Integer, E> enumValueMap) {
    super(innerCodec.getCqlType(), enumClass);
    this.enumValueMap = enumValueMap;
    this.innerCodec = innerCodec;
  }

  @Override
  public ByteBuffer serialize(E value, ProtocolVersion protocolVersion) throws InvalidTypeException {
    return innerCodec.serialize(value == null ? null : value.value(), protocolVersion);
  }

  @Override
  public E deserialize(ByteBuffer bytes, ProtocolVersion protocolVersion) throws InvalidTypeException {
    Integer value = innerCodec.deserialize(bytes, protocolVersion);
    return value == null ? null : enumValueMap.get(value);
  }

  @Override
  public E parse(String value) throws InvalidTypeException {
    return value == null || value.isEmpty() || value.equalsIgnoreCase("NULL") ? null : enumValueMap.get(Integer.parseInt(value));
  }

  @Override
  public String format(E value) throws InvalidTypeException {
    return value == null ? "NULL" : Integer.toString(value.value());
  }

}
