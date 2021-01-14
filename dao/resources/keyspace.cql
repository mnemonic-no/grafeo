CREATE KEYSPACE IF NOT EXISTS act WITH replication = { 'class':'SimpleStrategy', 'replication_factor':1 } AND durable_writes=true;

DROP TABLE IF EXISTS act.namespace;
DROP INDEX IF EXISTS act.origin_name_index;
DROP TABLE IF EXISTS act.origin;
DROP INDEX IF EXISTS act.object_type_name_index;
DROP TABLE IF EXISTS act.object_type;
DROP TABLE IF EXISTS act.object;
DROP TABLE IF EXISTS act.object_fact_binding;
DROP TABLE IF EXISTS act.object_by_type_value;
DROP INDEX IF EXISTS act.fact_type_name_index;
DROP TABLE IF EXISTS act.fact_type;
DROP TABLE IF EXISTS act.fact;
DROP TABLE IF EXISTS act.fact_comment;
DROP TABLE IF EXISTS act.fact_acl;
DROP TABLE IF EXISTS act.meta_fact_binding;
DROP TABLE IF EXISTS act.fact_by_timestamp;
DROP TABLE IF EXISTS act.evidence_submission;
DROP TABLE IF EXISTS act.evidence_submission_acl;
DROP TABLE IF EXISTS act.evidence;
DROP TABLE IF EXISTS act.fact_evidence_binding;

CREATE TABLE IF NOT EXISTS act.namespace (
  id UUID,
  name VARCHAR,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS act.origin (
  id UUID,
  namespace_id UUID,
  organization_id UUID,
  name VARCHAR,
  description VARCHAR,
  trust FLOAT,
  type INT,
  flags SET<INT>,
  PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS origin_name_index ON act.origin (name);

CREATE TABLE IF NOT EXISTS act.object_type (
  id UUID,
  namespace_id UUID,
  name VARCHAR,
  validator VARCHAR,
  validator_parameter VARCHAR,
  PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS object_type_name_index ON act.object_type (name);

CREATE TABLE IF NOT EXISTS act.object (
  id UUID,
  type_id UUID,
  value VARCHAR,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS act.object_fact_binding (
  object_id UUID,
  fact_id UUID,
  direction INT,
  PRIMARY KEY (object_id, fact_id)
) WITH CLUSTERING ORDER BY (fact_id ASC);

CREATE TABLE IF NOT EXISTS act.object_by_type_value (
  object_type_id UUID,
  object_value VARCHAR,
  object_id UUID,
  PRIMARY KEY ((object_type_id, object_value))
);

CREATE TABLE IF NOT EXISTS act.fact_type (
  id UUID,
  namespace_id UUID,
  name VARCHAR,
  validator VARCHAR,
  validator_parameter VARCHAR,
  relevant_object_bindings VARCHAR,
  relevant_fact_bindings VARCHAR,
  default_confidence FLOAT,
  PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS fact_type_name_index ON act.fact_type (name);

CREATE TABLE IF NOT EXISTS act.fact (
  id UUID,
  type_id UUID,
  value VARCHAR,
  in_reference_to_id UUID,
  organization_id UUID,
  source_id UUID,
  added_by_id UUID,
  access_mode INT,
  confidence FLOAT,
  trust FLOAT,
  timestamp BIGINT,
  last_seen_timestamp BIGINT,
  bindings VARCHAR,
  flags SET<INT>,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS act.fact_comment (
  fact_id UUID,
  id UUID,
  reply_to_id UUID,
  source_id UUID,
  comment VARCHAR,
  timestamp BIGINT,
  PRIMARY KEY (fact_id, id)
) WITH CLUSTERING ORDER BY (id ASC);

CREATE TABLE IF NOT EXISTS act.fact_acl (
  fact_id UUID,
  id UUID,
  subject_id UUID,
  source_id UUID,
  timestamp BIGINT,
  PRIMARY KEY (fact_id, id)
) WITH CLUSTERING ORDER BY (id ASC);

CREATE TABLE IF NOT EXISTS act.meta_fact_binding (
  fact_id UUID,
  meta_fact_id UUID,
  PRIMARY KEY (fact_id, meta_fact_id)
) WITH CLUSTERING ORDER BY (meta_fact_id ASC);

CREATE TABLE IF NOT EXISTS act.fact_by_timestamp (
  hour_of_day BIGINT,
  timestamp BIGINT,
  fact_id UUID,
  PRIMARY KEY (hour_of_day, timestamp, fact_id)
) WITH CLUSTERING ORDER BY (timestamp ASC);

CREATE TABLE IF NOT EXISTS act.evidence_submission (
  id UUID,
  name VARCHAR,
  data_type VARCHAR,
  media_type VARCHAR,
  length BIGINT,
  timestamp BIGINT,
  observation_timestamp BIGINT,
  source_id UUID,
  access_mode INT,
  checksum VARCHAR,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS act.evidence_submission_acl (
  submission_id UUID,
  id UUID,
  subject_id UUID,
  source_id UUID,
  timestamp BIGINT,
  PRIMARY KEY (submission_id, id)
) WITH CLUSTERING ORDER BY (id ASC);

CREATE TABLE IF NOT EXISTS act.evidence (
  checksum VARCHAR,
  data VARCHAR,
  PRIMARY KEY (checksum)
);

CREATE TABLE IF NOT EXISTS act.fact_evidence_binding (
  fact_id UUID,
  submission_id UUID,
  PRIMARY KEY (fact_id, submission_id)
) WITH CLUSTERING ORDER BY (submission_id ASC);