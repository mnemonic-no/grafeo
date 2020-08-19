# Migration notes
This file contains migrations which are required to be performed when upgrading the application code to a newer version.
It is not necessary to perform these steps when installing the application for the first time.

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
