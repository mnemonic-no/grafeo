package no.mnemonic.act.platform.dao.cassandra.entity;

public interface CassandraEntity {
  String KEY_SPACE = "act";
  String READ_CONSISTENCY = "LOCAL_QUORUM";
  String WRITE_CONSISTENCY = "LOCAL_QUORUM";
}
