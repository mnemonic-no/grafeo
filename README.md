Semi-Automated Cyber Threat Intelligence - ACT Platform
=======================================================

[*Semi-Automated Cyber Threat Intelligence*](https://www.mnemonic.no/research-and-development/semi-automated-cyber-threat-intelligence/) (ACT) is a research project led by mnemonic as with contributions from the University of Oslo, NTNU, Norwegian Security Authority (NSM), KraftCERT and FinansCERT.

The main objective of the ACT project is to develop a platform for cyber threat intelligence to uncover cyber attacks, cyber espionage and sabotage.
The project will result in new methods for data enrichment and data analysis to enable identification of threat agents, their motives, resources and attack methodologies.
In addition, the project will develop new methods, work processes and mechanisms for creating and distributing threat intelligence and countermeasures to stop ongoing and prevent future attacks.

In this repository the code of the ACT platform is published under an Open Source license.

## Usage

The ACT platform exposes a set of REST APIs. See this [guideline](https://bia.mnemonic.no/x/PYEp) on how to work with the API.

## Installation

##### Prerequisites

* A running installation of [Apache Cassandra](https://cassandra.apache.org/).
* Import the Cassandra database schema from `executable/testsrc/resources/setup.cql`.

##### Compilation

At this early stage of the project no compiled bundles are published, thus, you have to compile the platform yourself.
Just execute `mvn clean install -f parent/pom.xml` from the repository's root folder.
Add `-DskipTests` to the build command in order to not run the unit tests during build.
This will create an executable JAR bundle under `executable/target` containing all dependencies.

##### Configuration

* The application is configured with a properties file. See `executable/application.properties.localhost` as an example.
This configuration needs to point to your Cassandra installation.
* Access control including users and organizations is defined in another properties file.
See `executable/acl.properties.localhost` as an example and the [specification](https://bia.mnemonic.no/x/SIEp) for more details.
Make sure that your application configuration points to this properties file as well.

##### Execution

* After all steps above executing the application is just a matter of running this command:

```
java -Dapplication.properties.file=$PROPERTIES -jar $EXECUTABLE guice module=no.mnemonic.act.platform.rest.RestModule module=no.mnemonic.act.platform.service.ServiceModule
```

* Point $PROPERTIES and $EXECUTABLE to your configuration file and compiled JAR bundle, respectively.
* Alternatively, you can adapt `executable/init.sh` for your needs instead of executing the JAR bundle manually.
* If everything is configured correctly running the command above will start up the whole application stack and the API server will start listening for requests on the port specified in the configuration file.

## Requirements

* Java 8 for running the application.
* Maven for managing dependencies, building the code, running the unit tests, etc.
* An installation of [Apache Cassandra](https://cassandra.apache.org/) for storage.

## Known issues

See [Issues](https://github.com/mnemonic-no/act-platform/issues).

## Contributing

See the [CONTRIBUTING.md](CONTRIBUTING.md) file.

## License

The ACT platform is released under the ISC License. See the bundled LICENSE file for details.