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
    * use `bin/tnt4j-streams.bat` or `bin/tnt4j-streams.sh` to run standalone application
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

Sample files can be found in `samples/syslog-daemon` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="SyslogEventParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivitySyslogEventParser">
        <!--<property name="LocPathDelim" value="."/>-->

        <field name="EventType" locator="EventType" locator-type="Label"/>
        <field name="EventName" locator="EventName" locator-type="Label"/>
        <field name="Exception" locator="Exception" locator-type="Label"/>
        <field name="UserName" locator="UserName" locator-type="Label"/>
        <field name="ResourceName" locator="ResourceName" locator-type="Label"/>
        <field name="Location" locator="Location" locator-type="Label"/>
        <field name="Tag" locator="Tag" locator-type="Label"/>
        <field name="Correlator" locator="Correlator" locator-type="Label"/>
        <field name="ProcessId" locator="ProcessId" locator-type="Label"/>
        <field name="ThreadId" locator="ThreadId" locator-type="Label"/>
        <field name="Message" locator="Message" locator-type="Label"/>
        <field name="Severity" locator="Severity" locator-type="Label"/>
        <field name="ApplName" locator="ApplName" locator-type="Label"/>
        <field name="ServerName" locator="ServerName" locator-type="Label"/>
        <field name="EndTime" locator="EndTime" locator-type="Label" datatype="Timestamp" units="Microseconds"/>
        <field name="ElapsedTime" locator="ElapsedTime" locator-type="Label"/>
        <field name="MsgCharSet" locator="MsgCharSet" locator-type="Label"/>

        <!-- custom Syslog properties -->
        <field name="facility" locator="facility" locator-type="Label"/>
        <field name="level" locator="level" locator-type="Label" value-type="id"/>
        <field name="hostname" locator="hostname" locator-type="Label"/>
        <field name="hostaddr" locator="hostaddr" locator-type="Label"/>

        <!-- properties from Syslog message/structured data -->
        <!-- automatically puts all resolved map entries as custom activity properties -->
        <field name="SyslogMap" locator="SyslogMap" locator-type="Label"/>
        <field name="SyslogVars" locator="SyslogVars" locator-type="Label"/>

        <!-- if particular entries needed then use manual mapping like this-->
        <!--<field name="propName1" locator="SyslogVars.propName1" locator-type="Label"/>-->
        <!--<field name="propName2" locator="SyslogVars.propName2" locator-type="Label" datatype="Number" format="####0.00"/>-->
        <!--<field name="propName3" locator="SyslogMap.propName3" locator-type="Label"/>-->
    </parser>

    <stream name="SampleSyslogdStream" class="com.jkoolcloud.tnt4j.streams.inputs.SyslogdStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Protocol" value="tcp"/>
        <property name="Host" value="0.0.0.0"/>
        <property name="Port" value="514"/>
        <property name="Timeout" value="0"/>

        <parser-ref name="SyslogEventParser"/>
    </stream>   
</tnt-data-source>
```

Stream configuration states that `SyslogdStream` referencing parser `SyslogEventParser` shall be used.

`SyslogdStream` starts Syslogd server, to receive Syslog clients sent log events, on machine host and port defined by `Host` and `Port` 
properties. `Protocol` property defines protocol used for communication: may be `tcp` or `udp`. `Timeout` property defines communication 
timeout value ans is applicable if property `Protocol` has value `tcp`. `HaltIfNoParser` property states that stream should skip unparseable 
entries and don't stop if such situation occurs.

`SyslogEventParser` parser collects data from RFC 3164 or 5424 compliant Syslogd events and fills activity event fields from resolved log 
entry attributes map data.

Parser resolved data map may contain such entries:
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
    * `Severity` - resolved log event level mapped to `OpLevel`
    * `ApplName` - resolved log event application name
    * `ServerName` - resolved log event host name
    * `EndTime` - resolved log event timestamp value in microseconds
    * `ElapsedTime` - calculated time difference between same host and app events in microseconds
    * `MsgCharSet` - resolved log event char set name
 * for activity properties:
    * `facility` - resolved log event facility name
    * `level` - resolved log event level
    * `hostname` - resolved log event host name
    * `hostaddr` - resolved log event host address    
 * maps of resolved additional custom activity properties:
    * `SyslogMap` - map of resolved RFC 5424 structured data
    * `SyslogVars` - map of resolved application message contained (varName=varValue) variables

By default stream will put all resolved values form `SyslogMap` and `SyslogVars` as activity event properties. It is useful when all 
resolved data is "interesting" and particular set of those additional attributes is unknown.   

But if You know possible content of those maps, may select just some particular set of "interesting" entries of those maps to stream. In 
this case comment out field mappings for `SyslogMap` and `SyslogVars`, and put activity event mappings like this:  
```xml
    <field name="propName1" locator="SyslogVars.propName1" locator-type="Label"/>
    <field name="propName2" locator="SyslogVars.propName2" locator-type="Label" datatype="Number" format="####0.00"/>
    <field name="propName3" locator="SyslogMap.propName3" locator-type="Label"/>
```  

#### Syslog log file

This sample shows how to stream activity events from Syslog log file(s) entries.

Sample files can be found in `samples/syslog-file` directory.

`syslog.log` and `syslog2.log` files are sample Syslog log file depicting some Unix running machine activity.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="SyslogMessageParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivitySyslogLineParser">
        <property name="CharSet" value="UTF-8"/>

        <field name="EventType" locator="EventType" locator-type="Label"/>
        <field name="EventName" locator="EventName" locator-type="Label"/>
        <field name="Exception" locator="Exception" locator-type="Label"/>
        <field name="UserName" locator="UserName" locator-type="Label"/>
        <field name="ResourceName" locator="ResourceName" locator-type="Label"/>
        <field name="Location" locator="Location" locator-type="Label"/>
        <field name="Tag" locator="Tag" locator-type="Label"/>
        <field name="Correlator" locator="Correlator" locator-type="Label"/>
        <field name="ProcessId" locator="ProcessId" locator-type="Label"/>
        <field name="ThreadId" locator="ThreadId" locator-type="Label"/>
        <field name="Message" locator="Message" locator-type="Label"/>
        <field name="Severity" locator="Severity" locator-type="Label"/>
        <field name="ApplName" locator="ApplName" locator-type="Label"/>
        <field name="ServerName" locator="ServerName" locator-type="Label"/>
        <field name="EndTime" locator="EndTime" locator-type="Label" datatype="Timestamp" units="Microseconds"/>
        <field name="ElapsedTime" locator="ElapsedTime" locator-type="Label"/>
        <field name="MsgCharSet" locator="MsgCharSet" locator-type="Label"/>

        <!-- custom Syslog properties -->
        <field name="facility" locator="facility" locator-type="Label"/>
        <field name="level" locator="level" locator-type="Label" value-type="id"/>
        <field name="hostname" locator="hostname" locator-type="Label"/>
        <field name="version" locator="version" locator-type="Label"/>
        <field name="priority" locator="priority" locator-type="Label"/>

        <!-- properties from Syslog message/structured data -->
        <!-- automatically puts all resolved map entries as custom activity properties -->
        <field name="SyslogMap" locator="SyslogMap" locator-type="Label"/>
        <field name="SyslogVars" locator="SyslogVars" locator-type="Label"/>

        <!-- if particular entries needed then use manual mapping like this-->
        <!--<field name="propName1" locator="SyslogVars.propName1" locator-type="Label"/>-->
        <!--<field name="propName2" locator="SyslogVars.propName2" locator-type="Label" datatype="Number" format="####0.00"/>-->
        <!--<field name="propName3" locator="SyslogMap.propName3" locator-type="Label"/>-->
    </parser>

    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.FileLineStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="FileName" value="./samples/syslog-file/syslog.log"/>
        <property name="RestoreState" value="false"/>

        <parser-ref name="SyslogMessageParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `FileLineStream` referencing `SyslogMessageParser` shall be used.

`FileStream` reads data from `syslog.log` file. `HaltIfNoParser` property states that stream should skip unparseable
entries and don't stop if such situation occurs.

`SyslogMessageParser` parser reads RFC 3164 or 5424 compliant log lines and fills activity event fields from resolved log entry attributes 
map data.

`CharSet` property defines parser used char set.

Parser resolved data map may contain such entries:
 * for activity fields:
    * `EventType` - resolved from log line application message contained variable named `opt`
    * `EventName` - resolved log line facility name, or application message contained variable named `opn`
    * `Exception` - resolved from log line application message contained variable named `exc`
    * `UserName` - resolved from log line application message contained variable named `usr`
    * `ResourceName` - resolved log line application name, or application message contained variable named `rsn`
    * `Location` - resolved log line host name, or application message contained variable named `loc`
    * `Tag` - resolved set of values {`host name`, `application name`} for RFC 3164 and set of values {`facility name`, `host name`, 
    `application name`, `message id`} for RFC 5424, or application message contained variable named `tag`
    * `Correlator` - resolved from log line application message contained variable named `cid`
    * `ProcessId` - resolved log line process id
    * `ThreadId` - same as `ProcessId`
    * `Message` - resolved log line application message
    * `Severity` - resolved log line level mapped to `OpLevel`
    * `ApplName` - resolved log line application name
    * `ServerName` - resolved log line host name
    * `EndTime` - resolved log line timestamp value in microseconds
    * `ElapsedTime` - calculated time difference between same host and app events in microseconds
    * `MsgCharSet` - char set name used by parser
 * for activity properties:
    * `facility` - resolved log line facility name. If resolved `priority` is `null` - then value is `USER`
    * `level` - resolved log line level. If resolved `priority` is `null` - then value is `6` (`INFO`)
    * `hostname` - resolved log line host name
    * `version` - resolved log line Syslog version (`0` for RFC 3164, `1` for RFC 5424)
    * `priority` - resolved log line priority
 * maps of resolved additional custom activity properties:
    * `SyslogMap` - map of resolved RFC 5424 structured data
    * `SyslogVars` - map of resolved application message contained (varName=varValue) variables

By default stream will put all resolved values form `SyslogMap` and `SyslogVars` as activity event properties. It is useful when all 
resolved data is "interesting" and particular set of those additional attributes is unknown.   

But if You know possible content of those maps, may select just some particular set of "interesting" entries of those maps to stream. In 
this case comment out field mappings for `SyslogMap` and `SyslogVars`, and put activity event mappings like this:  
```xml
    <field name="propName1" locator="SyslogVars.propName1" locator-type="Label"/>
    <field name="propName2" locator="SyslogVars.propName2" locator-type="Label" datatype="Number" format="####0.00"/>
    <field name="propName3" locator="SyslogMap.propName3" locator-type="Label"/>
```  

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

All other required dependencies are defined in project modules `pom.xml` files. If maven is running
online mode it should download these defined dependencies automatically.

### Manually installed dependencies

**NOTE:** If you have build and installed TNT4J-Streams into Your local maven repository, you don't need to install
it manually.

Some of required and optional dependencies may be not available in public [Maven Repository](http://repo.maven.apache.org/maven2/). In this 
case we would recommend to download those dependencies manually into `lib` directory and install into local maven repository by running 
maven script `lib/pom.xml` with `package` goal.

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
1. if `tnt4j-streams` not built yet build it: run `mvn clean install` for a `pom.xml` file located in `tnt4j-streams` directory. 
2. now you can build `tnt4j-streams-syslogd`: run `mvn clean install` for a `pom.xml` file located in `tnt4j-streams-syslogd` directory. 

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
