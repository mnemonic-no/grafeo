# Migration notes
This file contains migrations which are required to be performed when upgrading the application code to a newer version.
It is not necessary to perform these steps when installing the application for the first time.

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
