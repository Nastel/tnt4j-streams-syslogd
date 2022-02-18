#### Syslog daemon (Syslogd)

This sample shows how to stream activity events from Syslogd received log events data. `SyslogdStream` starts Syslogd server depending on 
defined configuration.

Sample stream configuration: [`tnt-data-source.xml`](./tnt-data-source.xml)

Stream configuration states that `SyslogdStream` referencing parser `SyslogEventParser` shall be used.

`SyslogdStream` starts Syslogd server, to receive Syslog clients sent log events, on machine host and port defined by `Host` and `Port` 
properties. `Protocol` property defines protocol used for communication: may be `tcp` or `udp`. `Timeout` property defines communication 
timeout value ans is applicable if property `Protocol` has value `tcp`. `HaltIfNoParser` property states that stream should skip unparseable 
entries and don't stop if such situation occurs.

`SyslogEventParser` parser collects data from [`RFC 3164`](https://tools.ietf.org/html/rfc3164) or [`RFC 5424`](https://tools.ietf.org/html/rfc5424) 
compliant Syslogd events and fills activity event fields from resolved log entry attributes map data.

`SuppressMessagesLevel` property defines Syslog messages suppression level.
`SuppressIgnoredFields` property defines Syslog message ignored fields list used to compare if message contents are same. **NOTE:** field 
names are delimited using `|` symbol. 
`SuppressCacheSize` property defines maximal Syslog messages suppression cache entries count.
`SuppressCacheExpireDurationMinutes` property defines Syslog messages suppression cache entries expiration duration value in minutes.

Parser resolved data map may contain such entries:
 * for activity fields:
    * `EventType` - resolved from log event application message contained variable named `opt`
    * `EventName` - resolved log event facility name, or application message contained variable named `opn`
    * `Exception` - resolved from log event application message contained variable named `exc`
    * `UserName` - resolved from log event application message contained variable named `usr`
    * `ResourceName` - resolved log event application name, or application message contained variable named `rsn`
    * `Location` - resolved log event host name/address, or application message contained variable named `loc`
    * `Tag` - resolved set of values {`host name`, `application name`} for [`RFC 3164`](https://tools.ietf.org/html/rfc3164) and set of 
    values {`facility name`, `host name`, `application name`, `message id`} for [`RFC 5424`](https://tools.ietf.org/html/rfc5424), or 
    application message contained variable named `tag`
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
    * `SyslogMap` - map of resolved [`RFC 5424`](https://tools.ietf.org/html/rfc5424) structured data
    * `SyslogVars` - map of resolved application message contained (varName=varValue) variables

By default stream will put all resolved values form `SyslogMap` and `SyslogVars` as activity event properties. It is useful when all 
resolved data is "interesting" and particular set of those additional attributes is unknown.

But if you know possible content of those maps, may select just some particular set of "interesting" entries of those maps to stream. In 
this case comment out field mappings for `SyslogMap` and `SyslogVars`, and put activity event mappings like this:
```xml
<field name="propName1" locator="SyslogVars.propName1" locator-type="Label"/>
<field name="propName2" locator="SyslogVars.propName2" locator-type="Label" datatype="Number" format="####0.00"/>
<field name="propName3" locator="SyslogMap.propName3" locator-type="Label"/>
```
