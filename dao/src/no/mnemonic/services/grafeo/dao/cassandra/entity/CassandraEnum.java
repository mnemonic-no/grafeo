package no.mnemonic.services.grafeo.dao.cassandra.entity;

public interface CassandraEnum<E extends Enum<E>> {

  int value();

}
