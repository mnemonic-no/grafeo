# Changelog
All notable changes to this project will be documented in this file. Also see [MIGRATIONS.md](MIGRATIONS.md) when upgrading the ACT platform to a newer version.

The format is inspired by [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Origin, Trust & Confidence] - WIP
### Added
- REST endpoint for retrieval of an Origin by ID, `GET /v1/origin/uuid/{id}`.
- REST endpoint for listing Origins, `GET /v1/origin`.
- REST endpoint for creating Origins, `POST /v1/origin`.
- REST endpoint for updating Origins, `PUT /v1/origin/uuid/{id}`.
- REST endpoint for deleting Origins, `DELETE /v1/origin/uuid/{id}`.

### Changed
- Extend FactType API to include `defaultConfidence` field.
