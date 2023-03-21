package no.mnemonic.services.grafeo.dao.cassandra.mapper;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.mapper.MapperBuilder;
import com.datastax.oss.driver.api.mapper.annotations.DaoFactory;
import com.datastax.oss.driver.api.mapper.annotations.Mapper;

@Mapper
public interface CassandraMapper {

  @DaoFactory
  FactDao getFactDao();

  @DaoFactory
  FactTypeDao getFactTypeDao();

  @DaoFactory
  ObjectDao getObjectDao();

  @DaoFactory
  ObjectTypeDao getObjectTypeDao();

  @DaoFactory
  OriginDao getOriginDao();

  static MapperBuilder<CassandraMapper> builder(CqlSession session) {
    return new CassandraMapperBuilder(session);
  }
}
