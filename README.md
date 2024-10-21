# Tahopen Community Data Access

**CDA** is a Data Access tool that offers an abstraction layer to accessing data within CDF and CDE dashboards

**CDA** is one of the _tools_ of the **CTools** family and it is shipped within Tahopen BA Server

#### Pre-requisites for building the project:
* Maven, version 3+
* Java JDK 1.8

#### Building it

This is a maven project, and to build it use the following command
```
mvn clean install
```
The build result will be a Pentaho Plugin located in *assemblies/cda/target/cda-**.zip*. Then, this package can be dropped inside your system folder.
