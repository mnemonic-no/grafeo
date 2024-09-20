ACT Platform is now called grafeo!
==================================

Grafeo is a versatile platform to create custom data models, collect and share knowledge. It is a flexible, well-rounded
solution that can be used for any purpose. Developed as part of the [ACT research project](https://www.mnemonic.no/research-and-development/semi-automated-cyber-threat-intelligence/),
it remains at the core of ACT and powers the ACT backend. Learn more about ACT [here](https://github.com/mnemonic-no/act).

## Usage

Grafeo exposes a set of REST APIs. See this [guideline](https://github.com/mnemonic-no/grafeo/wiki/REST-API-Usage-Guideline) on how to work with the API.

## Installation

##### Prerequisites

* Java 17 or newer for running the application.
* Maven for managing dependencies, building the code, running the unit tests, etc.
* An installation of [Apache Cassandra](https://cassandra.apache.org/) for storage. Any version of Apache Cassandra 3.x or 4.x is supported.
* An installation of [Elasticsearch](https://www.elastic.co/products/elasticsearch) for indexing. Version 7.17 of Elasticsearch is required.
* (Optional) An installation of [Apache Kafka](https://kafka.apache.org/) for data centre replication.
* (Optional) An installation of [Docker](https://www.docker.com/) for running the integration tests.

##### Compilation

* Execute `mvn clean install -DskipTests` from the repository's root folder to compile the code.
* Afterwards follow the [deployment guide](https://github.com/mnemonic-no/grafeo/wiki/Architecture-and-Deployment-Guide) to run the application.

##### Testing

* Download a Cassandra image by `docker pull cassandra`.
* Download an Elasticsearch image by `docker pull docker.elastic.co/elasticsearch/elasticsearch:7.17.13`.
* Execute `mvn clean install` for running all tests including integration tests.
* Execute `mvn clean install -DskipSlowTests` for skipping the integration tests.
* By default the integration tests will try to connect to Docker on localhost and port 2375. Set the $DOCKER_HOST environment variable to override this behaviour.

## Known issues

See [Issues](https://github.com/mnemonic-no/grafeo/issues).

## Contributing

See the [CONTRIBUTING.md](CONTRIBUTING.md) file.

## License

Grafeo is released under the ISC License. See the bundled LICENSE file for details.