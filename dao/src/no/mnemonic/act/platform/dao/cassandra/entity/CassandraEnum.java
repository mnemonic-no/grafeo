package no.mnemonic.act.platform.dao.cassandra.entity;

public interface CassandraEnum<E extends Enum<E>> {

  int value();

}
