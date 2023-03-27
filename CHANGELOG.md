# Changelog
All notable changes to this project will be documented in this file. Also see [MIGRATIONS.md](MIGRATIONS.md) when upgrading the ACT platform to a newer version.

The format is inspired by [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Traverse endpoints] - 2020-06-18
### Added
- REST endpoint for traversing from a specific object, `POST /v1/traverse/object/{id}`
- REST endpoint for traversing from a specific object, `POST /v1/traverse/object/{type}/{value}`
- REST endpoint for traversing from set of objects, `POST /v1/traverse/objects`
- REST endpoint for traversing from object search, `POST /v1/traverse/objects/search`
- Traversal now supports filtering by time, retraction and limit 
- Traversal now exposes one-legged facts as object properties
- Traversal now exposes meta-facts and retraction status as fact properties
 
### Deprecated
- REST endpoint `POST /v1/object/{id}/traverse`, use `POST /v1/traverse/object/{id}`
- REST endpoint `POST /v1/object/{type}/{value}/traverse`, use `POST /v1/traverse/object/{type}/{value}`
- REST endpoint `POST /v1/object/traverse`, use `POST /v1/traverse/objects/search`

## [Origin, Trust & Confidence] - 2019-09-18
### Added
- REST endpoint for retrieval of an Origin by ID, `GET /v1/origin/uuid/{id}`.
- REST endpoint for listing Origins, `GET /v1/origin`.
- REST endpoint for creating Origins, `POST /v1/origin`.
- REST endpoint for updating Origins, `PUT /v1/origin/uuid/{id}`.
- REST endpoint for deleting Origins, `DELETE /v1/origin/uuid/{id}`.

### Changed
- Extend FactType API to include `defaultConfidence` field.
- Extend Fact API to include `trust`, `confidence`, `certainty` and `addedBy` fields.
- Extend Search APIs to filter on `trust`, `confidence` and `certainty`.
- Rename field `source` to `origin` in Fact and Search APIs (**breaking**).

## [Miscellaneous]
### Added
- (2023-03-23) Added support for `Grafeo-User-ID` HTTP header as a replacement for the `ACT-User-ID` HTTP header.
Clients need to replace `ACT-User-ID` with `Grafeo-User-ID`.
- (2023-02-28) Added parameters `minimumFactsCount` and `maximumFactsCount` to `POST /v1/object/search` and
`POST /v1/traverse/objects/search` to be able to filter Objects based on the amount of Facts bound to them.
- (2022-11-18) Added parameters `startTimestamp`, `endTimestamp`, `timeFieldStrategy` and `timeMatchStrategy` to all search
and traverse endpoints which accept a request body. Clients can use these parameters to control precisely how time search
will be executed. Deprecated `before` and `after` parameters in those endpoints.
- (2022-03-08) Added `lastSeenBy` field to Fact responses. This field denotes the user who saw the Fact last.
- (2021-10-18) Added `type` query parameter to `GET /v1/origin` endpoint to filter Origins by their type.
- (2021-10-15) Added a `NullValidator` which enforces that a value is unset (null). This validator can only be used with FactTypes.
- (2021-09-28) A new mechanism to check for already existing Facts has been implemented. To activate the new mechanism
add `act.fact.existence.check.use.cassandra=true` to the `application.properties` configuration file. The new mechanism
is recommended for new installations. For existing installations the configuration should be set to `false` to avoid the
creation of duplicated Facts. The old mechanism is deprecated and wil be removed in the future.
- (2021-06-03) Added optional `before` and `after` query parameters to endpoints fetching Objects by ID or type/value.
Facts with their `lastSeenTimestamp` outside the given time frame are omitted from statistics calculation.
- (2020-08-14) Added `includeStatistics` parameter to `POST /v1/object/search` endpoint to specify whether Object statistics
should be included in the response. The endpoint omits statistics by default now (**breaking**).
- (2020-06-26) Added configuration option `act.action.triggers.enabled` to enable/disable the action triggers framework.

### Changed
- (2023-01-06) When searching for Facts by time `timeFieldStrategy` and `timeMatchStrategy` will only be applied to daily indices.
When searching against the time global index only `endTimestamp` will be used (`startTimestamp` will be ignored) and the search
will always filter on `timestamp`.
- (2021-06-03) When searching for Objects the `before` and `after` parameters are now used to omit Facts with their
`lastSeenTimestamp` outside the given time frame from statistics calculation (similar to the search itself).
- (2021-02-19) All search and traverse endpoints with `before` and `after` parameters now filter Facts on their `lastSeenTimestamp`
instead of `timestamp` fields.
- (2020-08-14) The `POST /v1/object/search` and `POST /v1/fact/search` endpoints now return with a 403 if the search request
does not specify any search parameters. Users must provide at least one parameter (in addition to `limit`).
- (2020-04-17) In Create APIs (create Fact, create meta Fact, retract Fact) related entities (`organization`, `origin`, `acl`)
can be specified by either UUID or name. If an entity cannot be resolved a 412 response will be returned.
- (2020-04-17) When granting a Subject access to an existing Fact the Subject can be specified by either UUID or name.
If the Subject cannot be resolved a 412 response will be returned.
- (2019-11-06) If a user has the `unlimitedThreatIntelSearch` permission an unlimited number of results can be fetched when
searching for Facts. Otherwise the maximum number of returned results is capped at 10.000.
- (2019-11-04) Creating a new Fact where `source` and `destination` are the same Object will fail with a 412 response.
For this use case one-legged Facts with bi-directional binding should be created instead.
- (2019-09-25) In Search APIs resolve entities by name for `factType`, `objectType`, `origin` and `organization` before querying
ElasticSearch in order to avoid outdated names in the index. If an entity cannot be resolved a 412 response will be returned.

### Removed
- (2022-10-21) Removed handling of the legacy 'act' index. Ensure that all installations have reindexed their data into
daily indices, see [MIGRATIONS.md](MIGRATIONS.md).
- (2022-02-11) Removed the old mechanism to check for already existing Facts, including the configuration property
`act.fact.existence.check.use.cassandra` which is the new default (**breaking**). Facts added to installations before
October 2021 might be duplicated as a result. *It is recommended to set up installations older than October 2021 from
scratch and reimport existing data.*
