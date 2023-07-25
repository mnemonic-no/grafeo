# Migration notes
This file contains migrations which are required to be performed when upgrading the application code to a newer version.
It is not necessary to perform these steps when installing the application for the first time.

## [Rename configuration properties and environment variables] - 2023-07-10
All configuration properties and environment variables have been renamed. Change the configuration file `application.properties`
and replace the `act` prefix with `grafeo` in all properties. In addition, change all environment variables starting with
`ACT_PLATFORM_` by replacing that prefix with `GRAFEO_`.

## [Rename permission functions] - 2023-07-06
All permission functions have been renamed. Change the configuration file `acl.properties` according to the table below.

Old Name | New Name
-------- | --------
addThreatIntelType | addGrafeoType
updateThreatIntelType | updateGrafeoType
viewThreatIntelType | viewGrafeoType
addThreatIntelOrigin | addGrafeoOrigin
deleteThreatIntelOrigin | deleteGrafeoOrigin
updateThreatIntelOrigin | updateGrafeoOrigin
viewThreatIntelOrigin | viewGrafeoOrigin
addThreatIntelFact | addGrafeoFact
traverseThreatIntelFact | traverseGrafeoFact
viewThreatIntelFact | viewGrafeoFact
addThreatIntelFactComment | addGrafeoFactComment
viewThreatIntelFactComment | viewGrafeoFactComment
grantThreatIntelFactAccess | grantGrafeoFactAccess
viewThreatIntelFactAccess | viewGrafeoFactAccess
unlimitedThreatIntelSearch | unlimitedGrafeoSearch

## [Rename trigger events] - 2023-07-05
The service name of the generated trigger events has been renamed. Adjust the two configuration files `triggerEventDefinition.yaml`
and `triggerRule.yaml` and change the properties named `service` from `ThreatIntelligenceService` to `GrafeoService`.

## [New field in ElasticSearch mapping] - 2023-01-06
A new field has been added to the ElasticSearch mapping. Execute the following curl command against you ElasticSearch cluster
(or use Kibana) to update the existing mapping.
```
curl -X PUT "localhost:9200/act-time-global,act-daily-*/_mapping?include_type_name=false" -H 'Content-Type: application/json' -d'
{
  "properties": {
    "flags": {
      "type": "keyword"
    }
  }
}
'
```

The previous command only changes the mapping in ElasticSearch. In order to populate the new field for existing data also run the next curl command.
**Only apply this to the time global index!**
```
curl -X POST "localhost:9200/act-time-global/_update_by_query?conflicts=proceed" -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must_not": {
        "exists": {
          "field": "flags"
        }
      }
    }
  },
  "script": {
    "source": "ctx._source.flags = new String[]{\"TimeGlobalIndex\"}",
    "lang": "painless"
  }
}
'
```

## [Reindex data into daily indices] - 2022-06-22
In order to reindex data into daily indices execute the following commands. Build the code with Maven to create the `grafeo-cli-tools` application.

(1) Update ObjectTypes with the correct `indexOption` of `TimeGlobal`.  Execute the following CQL commands against your Cassandra cluster (e.g. using cqlsh).
```
# List all ObjectTypes with id, name and flags.
SELECT id, name, flags FROM object_type ;

# For each ObjectType which should have the TimeGlobal flag run:
UPDATE object_type SET flags = flags + {1} WHERE id = <id> ;
```

If the data model developed by mnemonic is used the following types should be updated:
campaign, country, organization, person, region, sector, subRegion, tactic, technique, threatActor, tool, toolType.

(2) Execute the following migration to correctly set the `TimeGlobalIndex` flag for Facts in Cassandra.
```
grafeo-cli-tools migrate timeGlobalFlag --conf=<path to application.properties file> --start=<start timestamp> --end=<end timestamp>
```

(3) Execute the following command against your ElasticSearch cluster to delete *all* existing daily indices and the time global index.
```
curl -X DELETE "localhost:9200/act-time-global,act-daily-*"
```

(4) Reindex data into daily indices and the time global index. This will recreate the previously deleted indices from scratch.
```
grafeo-cli-tools reindex --conf=<path to application.properties file> --start=<start timestamp> --end=<end timestamp>
```

Specify `--start` and `--end` based on the time frame you want to reindex. Depending on the amount of data steps (2) and (4) may take a while.

## [New flags field on fact_type and object_type tables] - 2022-03-11
A new field has been introduced on the `fact_type` and `object_type` tables in Cassandra.
Execute the following CQL commands against your Cassandra cluster (e.g. using cqlsh).

```
ALTER TABLE act.fact_type ADD flags SET<INT>;
ALTER TABLE act.object_type ADD flags SET<INT>;
```

## [Introduce fact_refresh_log table] - 2022-03-08
A new Cassandra table and a new field on the `fact` table have been introduced.
Execute the following CQL commands against your Cassandra cluster (e.g. using cqlsh).

```
ALTER TABLE act.fact ADD last_seen_by_id UUID;

CREATE TABLE IF NOT EXISTS act.fact_refresh_log (
  fact_id UUID,
  refreshed_timestamp BIGINT,
  refreshed_by_id UUID,
  PRIMARY KEY (fact_id, refreshed_timestamp)
) WITH CLUSTERING ORDER BY (refreshed_timestamp ASC);
```

Adapt and execute the `migrations/003-fact-refresh-log.py` script to populate the new table from the existing data.
Depending on the size of your cluster this may take a while.

Additionally, execute the following curl command against you ElasticSearch cluster (or use Kibana) to update the existing mapping with the new field as well.
```
curl -X PUT "localhost:9200/act/_mapping?include_type_name=false" -H 'Content-Type: application/json' -d'
{
  "properties": {
    "lastSeenByID": {
      "type": "keyword"
    }
  }
}
'
```

The previous command only changes the mapping in ElasticSearch. In order to populate the new field for existing data also run the next curl command.
This will update *all* existing data. Depending on the size of your cluster this may take a while.
```
curl -X POST "localhost:9200/act/_update_by_query?conflicts=proceed" -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must_not": {
        "exists": {
          "field": "lastSeenByID"
        }
      }
    }
  },
  "script": {
    "source": "ctx._source.lastSeenByID = ctx._source.addedByID",
    "lang": "painless"
  }
}
'
```

## [New field in ElasticSearch mapping] - 2022-02-17
A new field has been added to the ElasticSearch mapping. Execute the following curl command against you ElasticSearch cluster
(or use Kibana) to update the existing mapping.
```
curl -X PUT "localhost:9200/act/_mapping?include_type_name=false" -H 'Content-Type: application/json' -d'
{
  "properties": {
    "id": {
      "type": "keyword"
    }
  }
}
'
```

The previous command only changes the mapping in ElasticSearch. In order to populate the new field for existing data also run the next curl command.
This will update *all* existing data. Depending on the size of your cluster this may take a while.
```
curl -X POST "localhost:9200/act/_update_by_query?conflicts=proceed" -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must_not": {
        "exists": {
          "field": "id"
        }
      }
    }
  },
  "script": {
    "source": "ctx._source.id = ctx._id",
    "lang": "painless"
  }
}
'
```

## [New source_object_id and destination_object_id fields on fact table] - 2021-10-11
Two new fields have been introduced on the `fact` table in Cassandra. The application code is backwards-compatible with
existing data, however, the `bindings` field on that table is deprecated and will be removed in the future.

Execute the following CQL commands against your Cassandra cluster (e.g. using cqlsh).

```
ALTER TABLE act.fact ADD source_object_id UUID;
ALTER TABLE act.fact ADD destination_object_id UUID;
```

## [Introduce fact_existence lookup table] - 2021-09-20
A new Cassandra lookup table has been introduced. Execute the following CQL command against your Cassandra cluster (e.g. using cqlsh).

```
CREATE TABLE IF NOT EXISTS act.fact_existence (
  fact_hash ASCII,
  fact_id UUID,
  PRIMARY KEY (fact_hash)
);
```

## [HasAcl and HasComments flags] - 2021-06-25
Two new internal flags have been introduced in order to avoid unnecessary requests towards Cassandra.
Adapt and execute the `migrations/002-fact-has-acl-comments.py` script to update the existing data with the new flags.

## [Introduce fact_by_timestamp lookup table] - 2021-01-14
A new Cassandra lookup table has been introduced. Execute the following CQL command against your Cassandra cluster (e.g. using cqlsh).

```
CREATE TABLE IF NOT EXISTS act.fact_by_timestamp (
  hour_of_day BIGINT,
  timestamp BIGINT,
  fact_id UUID,
  PRIMARY KEY (hour_of_day, timestamp, fact_id)
) WITH CLUSTERING ORDER BY (timestamp ASC);
```

Adapt and execute the `migrations/001-fact-by-timestamp.py` script to populate the new table from the existing data.
Depending on the size of your cluster this may take a while.

## [Upgrade ElasticSearch to 7.8] - 2020-07-31
Follow the general upgrade guide to upgrade ElasticSearch to version 7.8: https://www.elastic.co/guide/en/elasticsearch/reference/7.8/setup-upgrade.html

Afterwards adjust the following cluster setting:
```
curl -X PUT "localhost:9200/_cluster/settings" -H 'Content-Type: application/json' -d'
{
  "persistent" : {
    "search.max_buckets" : 65535
  }
}
'
```

## [Rename configuration properties and functions] - 2020-06-05
Most configuration properties and functions have been renamed. Change the configuration files (`application.properties` and `acl.properties`) according to the tables below.

### Configuration properties
Old Name | New Name
-------- | --------
access.controller.properties.file | act.access.controller.properties.configuration.file
access.controller.read.interval | act.access.controller.properties.reload.interval
cassandra.data.center | act.cassandra.data.center
cassandra.port | act.cassandra.port
cassandra.contact.points | act.cassandra.contact.points
elasticsearch.port | act.elasticsearch.port
elasticsearch.contact.points | act.elasticsearch.contact.points
smb.client.url | act.smb.client.url
smb.client.username | act.smb.client.username
smb.client.password | act.smb.client.password
smb.queue.name | act.smb.queue.name
smb.server.url | act.smb.server.url
smb.server.username | act.smb.server.username
smb.server.password | act.smb.server.password
api.server.port | act.api.server.port
cors.allowed.origins | act.api.cors.allowed.origins

### Functions
Old Name | New Name
-------- | --------
addTypes | addThreatIntelType
updateTypes | updateThreatIntelType
viewTypes | viewThreatIntelType
addOrigins | addThreatIntelOrigin
deleteOrigins | deleteThreatIntelOrigin
updateOrigins | updateThreatIntelOrigin
viewOrigins | viewThreatIntelOrigin
addFactObjects | addThreatIntelFact
traverseFactObjects | traverseThreatIntelFact
viewFactObjects | viewThreatIntelFact
addFactComments | addThreatIntelFactComment
viewFactComments | viewThreatIntelFactComment
grantFactAccess | grantThreatIntelFactAccess
viewFactAccess | viewThreatIntelFactAccess
unlimitedSearch | unlimitedThreatIntelSearch

## [Move retracted hint to Cassandra] - 2019-11-05
Moving the retracted hint from ElasticSearch to Cassandra requires changes to the Cassandra schema.

### Cassandra
Execute the following CQL command against your Cassandra cluster (e.g. using cqlsh).

```
ALTER TABLE act.fact ADD flags SET<INT>;
```

### Migrate data
In order to migrate existing data execute the following command. It fetches all documents from ElasticSearch where
the `retracted` flag is set and produces a text file with CQL commands to update the new `flags` field in Cassandra.

```
curl -XGET "http://localhost:9200/act/_search" -H 'Content-Type: application/json' -d'
{
  "query": {
    "term": {
      "retracted": {
        "value": "true"
      }
    }
  },
  "size": 10000
}' | jq -r '.hits.hits | .[]._id | "UPDATE act.fact SET flags = {0} WHERE id = \(.) ;"' > retracted.txt
```

Update Cassandra using `cqlsh -f retracted.txt`.

## [Origin, Trust & Confidence] - 2019-07-29
The support for origin, trust and confidence requires changes to the database schemas.

### Cassandra
Execute the following CQL commands against your Cassandra cluster (e.g. using cqlsh).

```
ALTER TABLE act.fact_type ADD default_confidence FLOAT;

ALTER TABLE act.fact DROP confidence_level;
ALTER TABLE act.fact ADD confidence FLOAT;
ALTER TABLE act.fact ADD trust FLOAT;
ALTER TABLE act.fact ADD added_by_id UUID;

DROP TABLE IF EXISTS act.source;
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
```

### ElasticSearch
Execute the following curl command against you ElasticSearch cluster (or use Kibana).

```
curl -X PUT "localhost:9200/act/_mapping?include_type_name=false" -H 'Content-Type: application/json' -d'
{
  "properties": {
    "addedByID": {
      "type": "keyword"
    },
    "addedByName": {
      "type": "keyword",
      "fields": {
        "text": {
          "type": "text"
        }
      }
    },
    "confidence": {
      "type": "half_float"
    },
    "trust": {
      "type": "half_float"
    },
    "certainty": {
      "type": "half_float"
    }
  }
}
'
```

The previous command only changes the mappings in Elasticsearch. In order to make the existing data available for search using the new fields also run the next curl command.
This will update *all* existing data to include default values for the new fields. Depending on the size of your cluster this may take a while.
```
curl -X POST "localhost:9200/act/_update_by_query?conflicts=proceed" -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must_not": {
        "exists": {
          "field": "certainty"
        }
      }
    }
  },
  "script": {
    "source": "ctx._source.confidence = 1.0 ; ctx._source.trust = 0.8 ; ctx._source.certainty = 0.8",
    "lang": "painless"
  }
}
'
```
