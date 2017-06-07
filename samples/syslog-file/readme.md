#### Syslog log file

This sample shows how to stream activity events from Syslog log file(s) entries.

[`syslog.log`](./syslog.log) and [`syslog2.log`](./syslog2.log) files are sample Syslog log file depicting some Unix running machine activity.

Sample stream configuration: [`tnt-data-source.xml`](./tnt-data-source.xml)

Stream configuration states that `FileLineStream` referencing `SyslogMessageParser` shall be used.

`FileStream` reads data from `syslog.log` file. `HaltIfNoParser` property states that stream should skip unparseable
entries and don't stop if such situation occurs.

`SyslogMessageParser` parser reads [`RFC 3164`](https://tools.ietf.org/html/rfc3164) or [`RFC 5424`](https://tools.ietf.org/html/rfc5424) 
compliant log lines and fills activity event fields from resolved log entry attributes map data.

`CharSet` property defines parser used char set.

`SuppressMessagesLevel` property defines Syslog messages suppression level.
`SuppressIgnoredFields` property defines Syslog message ignored fields list used to compare if message contents are same. **NOTE:** field 
names are delimited using `|` symbol. 
`SuppressCacheSize` property defines maximal Syslog messages suppression cache entries count.
`SuppressCacheExpireDurationMinutes` property defines Syslog messages suppression cache entries expiration duration value in minutes.

Parser resolved data map may contain such entries:
 * for activity fields:
    * `EventType` - resolved from log line application message contained variable named `opt`
    * `EventName` - resolved log line facility name, or application message contained variable named `opn`
    * `Exception` - resolved from log line application message contained variable named `exc`
    * `UserName` - resolved from log line application message contained variable named `usr`
    * `ResourceName` - resolved log line application name, or application message contained variable named `rsn`
    * `Location` - resolved log line host name, or application message contained variable named `loc`
    * `Tag` - resolved set of values {`host name`, `application name`} for [`RFC 3164`](https://tools.ietf.org/html/rfc3164) and set of 
    values {`facility name`, `host name`, `application name`, `message id`} for [`RFC 5424`](https://tools.ietf.org/html/rfc5424), or 
    application message contained variable named `tag`
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
    * `version` - resolved log line Syslog version (`0` for [`RFC 3164`](https://tools.ietf.org/html/rfc3164), `1` for [`RFC 5424`](https://tools.ietf.org/html/rfc5424))
    * `priority` - resolved log line priority
 * maps of resolved additional custom activity properties:
    * `SyslogMap` - map of resolved [`RFC 5424`](https://tools.ietf.org/html/rfc5424) structured data
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
