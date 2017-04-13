# tnt4j-streams-syslogd
TNT4J Streams for handling Syslog messages.

TNT4J-Streams-Syslogd is extension of TNT4J-Streams to give ability of streaming Syslog events/log entries as activity events to
JKoolCloud.

TNT4J-Streams-Syslogd is under LGPLv2.1 license as dependent Syslog4j itself.

This document covers just information specific to TNT4J-Streams-Syslogd project.
Detailed information on TNT4J-Streams can be found in [README document](https://github.com/Nastel/tnt4j-streams/blob/master/README.md).

Why TNT4J-Streams-Syslogd
======================================

 * Allows to stream activities parsed from Syslog daemon (Syslogd) events data.   
 * Allows to stream activities parsed from Syslog log files.
 
**NOTE:** Currently supports (RFC 3164) and the Structured Syslog protocol (RFC 5424).

Importing TNT4J-Streams-Syslogd project into IDE
======================================

## Eclipse
* Select File->Import...->Maven->Existing Maven Projects
* Click 'Next'
* In 'Root directory' field select path of directory where You have downloaded (checked out from git)
TNT4J-Streams project
* Click 'OK'
* Dialog fills in with project modules details
* Click 'Finish'

Running TNT4J-Streams-Syslogd
======================================

Also see TNT4J-Streams README document chapter ['Running TNT4J-Streams'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#running-tnt4j-streams).

## TNT4J-Streams-Syslogd can be run
* As standalone application
    * write streams configuration file. See ['Streams configuration'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#streams-configuration) chapter for more details
    * configure Your loggers
    * use [`bin/tnt4j-streams.bat`](./bin/tnt4j-streams.bat) or [`bin/tnt4j-streams.sh`](./bin/tnt4j-streams.sh) to run standalone application
* As API integrated into Your product
    * Write streams configuration file. See ['Streams configuration'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#streams-configuration) chapter for more details
    * use `StreamsAgent.runFromAPI(configFileName)` in your code

## Samples

### Running samples
When release assemblies are built, samples are located in `samples` directory i.e.
`../build/tnt4j-streams-syslogd/tnt4j-streams-syslogd-1.0.0/samples`.
To run desired sample:
* go to sample directory
* run `run.bat` or `run.sh` depending on Your OS

For more detailed explanation of streams and parsers configuration and usage see chapter ['Configuring TNT4J-Streams-Syslogd'](#configuring-tnt4j-streams-syslogd)
and JavaDocs.

#### Syslog daemon (Syslogd)

This sample shows how to stream activity events from Syslogd received log events data. `SyslogdStream` starts Syslogd server depending on 
defined configuration. 

Sample files can be found in [`samples/syslog-daemon`](./samples/syslog-daemon/) directory.

See sample [`readme.md`](/samples/syslog-daemon/readme.md) file for more details.

#### Syslog log file

This sample shows how to stream activity events from Syslog log file(s) entries.

Sample files can be found in [`samples/syslog-file`](./samples/syslog-file/) directory.

See sample [`readme.md`](/samples/syslog-file/readme.md) file for more details.

Configuring TNT4J-Streams-Syslogd
======================================

Details on TNT4J-Streams related configuration can be found in TNT4J-Streams README document chapter ['Configuring TNT4J-Streams'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#configuring-tnt4j-streams).

### Streams configuration

#### Syslogd stream parameters

 * Host - host name to run Syslog server. (Required)
 * Port - port number to run Syslog server. Default value - `514`. (Optional)
 * Protocol - protocol of Syslog server communication: one of `tcp` or `udp` . Default value - `tcp`. (Optional)
 * Timeout - server communication timeout, where `0` means - server implementation dependent timeout handling. Actual if `Protocol` property 
 value is set to `tcp`. Default value - `0`. (Optional)

    sample:
```xml
    <property name="Protocol" value="udp"/>
    <property name="Host" value="0.0.0.0"/>
    <property name="Port" value="5114"/>
    <property name="Timeout" value="60"/>
```

Also see ['Generic streams parameters'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#generic-streams-parameters) and ['Buffered streams parameters'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#buffered-streams-parameters).

### Parsers configuration

#### Activity Syslog event parser

This parser has no additional configuration parameters. 

Also see ['Activity map parser'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#activity-map-parser).

#### Activity Syslog line parser

 * CharSet - name of char set used by Syslog lines parser. Default value - `UTF-8`. (Optional)

    sample:
```xml
    <property name="CharSet" value="ISO-8859-1"/>
``` 

Also see ['Activity map parser'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#activity-map-parser).

How to Build TNT4J-Streams-Syslogd
=========================================

## Requirements
* JDK 1.7+
* [Apache Maven 3](https://maven.apache.org/)
* [TNT4J-Streams](https://github.com/Nastel/tnt4j-streams) `core` module in particular

All other required dependencies are defined in project [`pom.xml`](./pom.xml) file. If maven is running online mode it should download these 
defined dependencies automatically.

### Manually installed dependencies

**NOTE:** If you have build and installed TNT4J-Streams into Your local maven repository, you don't need to install
it manually.

`TNT4J-Streams-Syslogd` project does not require any manually downloaded dependencies at the moment.

**NOTE:** also see TNT4J-Streams README document chapter ['Manually installed dependencies'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#manually-installed-dependencies).

## Building
   * to build project and make release assemblies run maven goals `clean package`
   * to build project, make release assemblies and install to local repo run maven goals `clean install`

Release assemblies are built to `../build/tnt4j-streams-syslogd` directory.

**NOTE:** sometimes maven fails to correctly handle dependencies. If dependency configuration looks
fine, but maven still complains about missing dependencies try to delete local maven repository
by hand: i.e. delete contents of `c:\Users\[username]\.m2\repository` directory.

So resuming build process quick "how to build" steps would be like this:
1. if `tnt4j-streams` not built yet build it: run `mvn clean install` for a [`pom.xml`](https://github.com/Nastel/tnt4j-streams/blob/master/pom.xml) file located in 
`tnt4j-streams` directory. 
2. now you can build `tnt4j-streams-syslogd`: run `mvn clean install` for a [`pom.xml`](./pom.xml) file located in `tnt4j-streams-syslogd` 
directory. 

## Running samples

See 'Running TNT4J-Streams-Syslogd' chapter section ['Samples'](#samples).

Testing of TNT4J-Streams-Syslogd
=========================================

## Requirements
* [JUnit 4](http://junit.org/)
* [Mockito](http://mockito.org/)

## Testing using maven
Maven tests run is disabled by default. To enable Maven to run tests set Maven command line argument 
`-DskipTests=false`.

## Running manually from IDE
* in `syslogd` module run JUnit test suite named `AllSyslogdStreamTests`
