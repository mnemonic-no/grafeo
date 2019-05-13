Semi-Automated Cyber Threat Intelligence - ACT Platform
=======================================================

[*Semi-Automated Cyber Threat Intelligence*](https://www.mnemonic.no/research-and-development/semi-automated-cyber-threat-intelligence/) (ACT) is a research project led by mnemonic as with contributions from the University of Oslo, NTNU, Norwegian Security Authority (NSM), KraftCERT and Nordic Financial CERT.

The main objective of the ACT project is to develop a platform for cyber threat intelligence to uncover cyber attacks, cyber espionage and sabotage.
The project will result in new methods for data enrichment and data analysis to enable identification of threat agents, their motives, resources and attack methodologies.
In addition, the project will develop new methods, work processes and mechanisms for creating and distributing threat intelligence and countermeasures to stop ongoing and prevent future attacks.

In this repository the code of the ACT platform is published under an Open Source license.

## Usage

The ACT platform exposes a set of REST APIs. See this [guideline](https://github.com/mnemonic-no/act-platform/wiki/REST-API-Usage-Guideline) on how to work with the API.

## Installation

##### Prerequisites

* A running installation of [Apache Cassandra](https://cassandra.apache.org/). Any version of Apache Cassandra 3.x is support.
* A running installation of [Elasticsearch](https://www.elastic.co/products/elasticsearch). Version 6.6 of Elasticsearch is required.
* Import the Cassandra database schema from `deployment-combined/resources/cassandra.cql`.

##### Configuration

* The application is configured using a properties file. See `deployment-combined/examples/application.properties` as an example.
This configuration needs to point to your Cassandra and Elasticsearch installations.
* Access control including users and organizations is defined in another properties file.
See `deployment-combined/examples/acl.properties` as an example and the [specification](https://github.com/mnemonic-no/act-platform/wiki/Role-Based-Access-Control) for more details.
Make sure that your application configuration points to this properties file as well.
* The default configuration should work as long as Cassandra and Elasticsearch are installed on localhost and listen on their default ports.

##### Execution

At this early stage of the project no pre-build bundles are published, thus, you have to compile the platform yourself. Just execute `mvn clean install -DskipTests` from the repository's root folder.
This will create a tarball under `deployment-combined/target` containing everything needed to run the platform.
Extract this tarball and execute `bin/init.sh start` to start the platform. Stop the platform again with `bin/init.sh stop`.
On first start-up the example configuration files from the `examples` folder will be copied into the `conf` folder. Adjust the configuration to your needs.
If everything is configured correctly running the init script will start up the whole application stack and the API server will start listening for requests on the port specified in the configuration.
Check the log files under the `logs` folder for any error messages. Make sure that Cassandra and Elasticsearch are running and that the configuration points to them correctly.

##### Testing

* For running the integration tests install [Docker](https://www.docker.com/).
* Download a Cassandra image by `docker pull cassandra`.
* Download an Elasticsearch image by `docker pull docker.elastic.co/elasticsearch/elasticsearch:6.6.2`.
* By default the integration tests will try to connect to Docker on localhost and port 2375. You can set the $DOCKER_HOST environment variable to override this behaviour.
* Execute `mvn clean install` for running all tests including integration tests.
* Execute `mvn clean install -DskipSlowTests` for skipping the integration tests.

## Requirements

* Java 8 for running the application.
* Maven for managing dependencies, building the code, running the unit tests, etc.
* An installation of [Docker](https://www.docker.com/) for running the integration tests.
* An installation of [Apache Cassandra](https://cassandra.apache.org/) for storage.
* An installation of [Elasticsearch](https://www.elastic.co/products/elasticsearch) for indexing.

## Known issues

See [Issues](https://github.com/mnemonic-no/act-platform/issues).

## Contributing

See the [CONTRIBUTING.md](CONTRIBUTING.md) file.

## License

The ACT platform is released under the ISC License. See the bundled LICENSE file for details.