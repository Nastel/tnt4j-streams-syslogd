# tnt4j-streams-syslogd
TNT4J Streams for handling Syslog messages.

TNT4J-Streams-Syslogd is extension of TNT4J-Streams to give ability of streaming Syslog events/log entries as activity events to
[jKoolCloud](https://www.jkoolcloud.com).

TNT4J-Streams-Syslogd is under LGPLv2.1 license as dependent Syslog4j itself.

This document covers just information specific to TNT4J-Streams-Syslogd project.
Detailed information on TNT4J-Streams can be found in [README document](https://github.com/Nastel/tnt4j-streams/blob/master/README.md).

Why TNT4J-Streams-Syslogd
======================================

 * Allows to stream activities parsed from Syslog daemon (Syslogd) events data.
 * Allows to stream activities parsed from Syslog log files.

**NOTE:** Currently supports [`RFC 3164`](https://tools.ietf.org/html/rfc3164) and the Structured Syslog protocol [`RFC 5424`](https://tools.ietf.org/html/rfc5424).

Importing TNT4J-Streams-Syslogd project into IDE
======================================

## Eclipse
* Select File->Import...->Maven->Existing Maven Projects
* Click 'Next'
* In 'Root directory' field select path of directory where you have downloaded (checked out from git)
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
    * configure your loggers
    * use [`bin/tnt4j-streams.bat`](./bin/tnt4j-streams.bat) or [`bin/tnt4j-streams.sh`](./bin/tnt4j-streams.sh) to run standalone application
* As API integrated into your product
    * Use Maven dependency:
      ```xml
          <dependency>
              <groupId>com.jkoolcloud.tnt4j.streams</groupId>
              <artifactId>tnt4j-streams-syslogd</artifactId>
              <version>1.12.0</version>
          </dependency>
      ``` 
    * Write streams configuration file. See ['Streams configuration'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#streams-configuration) chapter for more details
    * use `StreamsAgent.runFromAPI(new CfgStreamsBuilder().setConfig(configFileName))` in your code

## Samples

### Running samples
When release assemblies are built, samples are located in [`samples`](./samples/) directory, e.g.,
`build/tnt4j-streams-syslogd-1.12.0/samples`.
To run desired sample:
* go to sample directory
* run `run.bat` or `run.sh` depending on your OS

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

 * `Host` - host name to run Syslog server. (Required)
 * `Port` - port number to run Syslog server. Default value - `514`. (Optional)
 * `Protocol` - protocol of Syslog server communication: one of `tcp` or `udp` . Default value - `tcp`. (Optional)
 * `Timeout` - server communication timeout, where `0` means - server implementation dependent timeout handling. Actual if `Protocol` 
 property value is set to `tcp`. Default value - `0`. (Optional)

    sample:
```xml
    <property name="Protocol" value="udp"/>
    <property name="Host" value="0.0.0.0"/>
    <property name="Port" value="5114"/>
    <property name="Timeout" value="60"/>
```

Also see ['Generic streams parameters'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#generic-streams-parameters) and ['Buffered streams parameters'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#buffered-streams-parameters).

### Parsers configuration

#### Abstract Syslog parser

 * `SuppressMessagesLevel` - Syslog messages suppression level:
    * `0` - output all Syslog messages
    * `-1` - output only the first occurrence of Syslog message
    * `any other positive number` - suppresses all Syslog messages except those that are multiples of that number

   Default value - `0`. (Optional)
 * `SuppressIgnoredFields` - Syslog message ignored fields list used to compare if message contents are same. Default value - [`EndTime`, 
 `ElapsedTime`, `Tag`]. (Optional)
 * `SuppressCacheSize` - maximal Syslog messages suppression cache entries count. Default value - `100`. (Optional)
 * `SuppressCacheExpireDurationMinutes` - Syslog messages suppression cache entries expiration duration value in minutes. 
 Default value - `10`. (Optional)
 * `FlattenStructuredData` - flag indicating to flatten structured data map if there is only one structure available. Default value - 
 `false`. (Optional)

    sample:
```xml
    <property name="SuppressMessagesLevel" value="10"/>
    <property name="SuppressIgnoredFields" value="EndTime|ElapsedTime|Tag"/>
    <property name="SuppressCacheSize" value="1000"/>
    <property name="SuppressCacheExpireDurationMinutes" value="30"/>
    <property name="FlattenStructuredData" value="true"/>
```

Also see ['Activity map parser'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#activity-map-parser).

#### Activity Syslog event parser

This parser has no additional configuration parameters.

Also see ['Abstract Syslog parser'](#abstract-syslog-parser).

This parser resolved data map may contain such entries:
 * for activity fields:
    * `EventType` - resolved from log event application message contained variable named `opt`
    * `EventName` - resolved log event facility name, or application message contained variable named `opn`
    * `Exception` - resolved from log event application message contained variable named `exc`
    * `UserName` - resolved from log event application message contained variable named `usr`
    * `ResourceName` - resolved log event application name, or application message contained variable named `rsn`
    * `Location` - resolved log event host name/address, or application message contained variable named `loc`
    * `Tag` - resolved set of values {`host name`, `application name`} for RFC 3164 and set of values {`facility name`, `host name`, 
    `application name`, `message id`} for RFC 5424, or application message contained variable named `tag`
    * `Correlator` - resolved from log event application message contained variable named `cid`
    * `ProcessId` - resolved log event process id
    * `ThreadId` - same as `ProcessId`
    * `Message` - resolved log event application message
    * `Severity` - resolved log event level mapped to value from [`OpLevel`](https://github.com/Nastel/TNT4J/blob/master/src/main/java/com/jkoolcloud/tnt4j/core/OpLevel.java) enumeration
    * `ApplName` - resolved log event application name`
    * `ServerName` - resolved log event host name
    * `EndTime` - resolved log event timestamp value in microseconds
    * `ElapsedTime` - calculated time difference between same host and app events in microseconds
    * `MsgCharSet` - resolved log event char set name

 * for activity properties:
    * `facility` - resolved log event facility name
    * `level` - resolved log event level
    * `hostname` - resolved log event host name
    * `hostaddr` - resolved log event host address
    * `priority` - resolved log line priority

 * maps of resolved additional custom activity properties:
    * `SyslogMap` - map of resolved RFC 5424 structured data: contains sub-map for every found structure, but can be flattened to single 
    level map (if only one structure is available) using parser property `FlattenStructuredData`
    * `SyslogVars` - map of resolved application message contained (varName=varValue) variables

#### Activity Syslog line parser

 * CharSet - name of char set used by Syslog lines parser. Default value - `UTF-8`. (Optional)

    sample:
```xml
    <property name="CharSet" value="ISO-8859-1"/>
```

Also see ['Abstract Syslog parser'](#abstract-syslog-parser).

This parser resolved data map may contain such entries:
 * for activity fields:
    * `EventType` - resolved from log line application message contained variable named `opt`
    * `EventName` - resolved log line facility name, or application message contained variable named `opn`
    * `Exception` - resolved from log line application message contained variable named `exc`
    * `UserName` - resolved from log line application message contained variable named `usr`
    * `ResourceName` - resolved log line application name, or application message contained variable named `rsn`
    * `Location` - resolved log line host name/address, or application message contained variable named `loc`
    * `Tag` - resolved set of values {`host name`, `application name`} for RFC 3164 and set of values {`facility name`, `host name`, 
    `application name`, `message id`} for RFC 5424, or application message contained variable named `tag`
    * `Correlator` - resolved from log line application message contained variable named `cid`
    * `ProcessId` - resolved log line process id
    * `ThreadId` - same as `ProcessId`
    * `Message` - resolved log line application message
    * `Severity` - resolved log line level mapped to value from [`OpLevel`](https://github.com/Nastel/TNT4J/blob/master/src/main/java/com/jkoolcloud/tnt4j/core/OpLevel.java) enumeration
    * `ApplName` - resolved log line application name
    * `ServerName` - resolved log line host name
    * `EndTime` - resolved log line timestamp value in microseconds
    * `ElapsedTime` - calculated time difference between same host and app events in microseconds
    * `MsgCharSet` - char set name used by parser

 * for activity properties:
    * `facility` - resolved log line facility name. If resolved `priority` is `null` - then value is `user`
    * `level` - resolved log line level. If resolved `priority` is `null` - then value is `INFO`
    * `hostname` - resolved log line host name
    * `version` - resolved log line Syslog version (`0` for `RFC 3164`, `1` for `RFC 5424`)
    * `priority` - resolved log line priority

 * maps of resolved additional custom activity properties:
    * `SyslogMap` - map of resolved RFC 5424 structured data: contains sub-map for every found structure, but can be flattened to single 
    level map (if only one structure is available) using parser property `FlattenStructuredData`
    * `SyslogVars` - map of resolved application message contained `varName=varValue` variables

How to Build TNT4J-Streams-Syslogd
=========================================

## Requirements
* JDK 1.8+
* [Apache Maven 3](https://maven.apache.org/)
* [TNT4J-Streams](https://github.com/Nastel/tnt4j-streams) `core` module in particular

All other required dependencies are defined in project [`pom.xml`](./pom.xml) file. If Maven is running online mode it should download these 
defined dependencies automatically.

### Manually installed dependencies

**NOTE:** If you have build and installed TNT4J-Streams into your local Maven repository, you don't need to install
it manually.

`TNT4J-Streams-Syslogd` project does not require any manually downloaded dependencies at the moment.

**NOTE:** also see TNT4J-Streams README document chapter ['Manually installed dependencies'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#manually-installed-dependencies).

## Building
* To build the project, run Maven goals `clean package`
* To build the project and install to local repo, run Maven goals `clean install`
* To make distributable release assemblies use one of profiles: `pack-bin` or `pack-all`:
    * containing only binary (including `test` package) distribution: run `mvn -P pack-bin`
    * containing binary (including `test` package), `source` and `javadoc` distribution: run `mvn -P pack-all`
* To make maven required `source` and `javadoc` packages, use profile `pack-maven`
* To make maven central compliant release having `source`, `javadoc` and all signed packages, use `maven-release` profile

Release assemblies are built to `build/` directory.

**NOTE:** sometimes Maven fails to correctly handle dependencies. If dependency configuration looks fine, but Maven still complains about 
missing dependencies try to delete local Maven repository by hand: e.g., on MS Windows delete contents of `c:\Users\[username]\.m2\repository` 
directory.

So resuming build process quick "how to build" steps would be like this:
1. if `tnt4j-streams` not built yet build it: run `mvn clean install` for a [`pom.xml`](https://github.com/Nastel/tnt4j-streams/blob/master/pom.xml) 
file located in `tnt4j-streams` directory. 
2. now you can build `tnt4j-streams-syslogd`: run `mvn clean install` for a [`pom.xml`](./pom.xml) file located in `tnt4j-streams-syslogd` 
directory.

## Running samples

See 'Running TNT4J-Streams-Syslogd' chapter section ['Samples'](#samples).

Testing of TNT4J-Streams-Syslogd
=========================================

## Requirements
* [JUnit 4](http://junit.org/)
* [Mockito](http://mockito.org/)

## Testing using Maven
Maven tests run is disabled by default. To enable Maven to run tests set Maven command line argument 
`-DskipTests=false`.

## Running manually from IDE
* in `syslogd` module run JUnit test suite named `AllSyslogdStreamTests`
