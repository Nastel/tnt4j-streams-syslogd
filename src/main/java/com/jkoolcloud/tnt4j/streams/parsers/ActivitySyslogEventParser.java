/*
 * Copyright (C) 2015-2017, JKOOL LLC.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.jkoolcloud.tnt4j.streams.parsers;

import static com.jkoolcloud.tnt4j.streams.fields.StreamFieldType.*;
import static com.jkoolcloud.tnt4j.streams.utils.SyslogStreamConstants.*;

import java.lang.Exception;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.MapUtils;
import org.graylog2.syslog4j.impl.message.structured.StructuredSyslogMessage;
import org.graylog2.syslog4j.server.SyslogServerEventIF;
import org.graylog2.syslog4j.server.impl.event.SyslogServerEvent;
import org.graylog2.syslog4j.server.impl.event.structured.StructuredSyslogServerEvent;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.SyslogUtils;
import com.jkoolcloud.tnt4j.tracker.TimeTracker;

/**
 * Implements an activity data parser that assumes each activity data item is an Syslog server event
 * {@link SyslogServerEventIF}. Parser resolved event data fields are put into {@link Map} and afterwards mapped into
 * activity fields and properties according to defined parser configuration.
 * <p>
 * Map entries containing values as internal {@link Map}s are automatically mapped into activity properties. If only
 * particular inner map entries are needed, then in parser fields mapping configuration define those properties as
 * separate fields.
 * <p>
 * This parser resolved data map may contain such entries:
 * <ul>
 * <li>for activity fields:</li>
 * <ul>
 * <li>EventType - resolved from log event application message contained variable named
 * '{@value com.jkoolcloud.tnt4j.logger.AppenderConstants#PARAM_OP_TYPE_LABEL}'</li>
 * <li>EventName - resolved log event facility name, or application message contained variable named
 * '{@value com.jkoolcloud.tnt4j.logger.AppenderConstants#PARAM_OP_NAME_LABEL}'</li>
 * <li>Exception - resolved from log event application message contained variable named
 * '{@value com.jkoolcloud.tnt4j.logger.AppenderConstants#PARAM_EXCEPTION_LABEL}'</li>
 * <li>UserName - resolved from log event application message contained variable named
 * '{@value com.jkoolcloud.tnt4j.logger.AppenderConstants#PARAM_USER_LABEL}'</li>
 * <li>ResourceName - resolved log event application name, or application message contained variable named
 * '{@value com.jkoolcloud.tnt4j.logger.AppenderConstants#PARAM_RESOURCE_LABEL}'</li>
 * <li>Location - resolved log event host name/address, or application message contained variable named
 * '{@value com.jkoolcloud.tnt4j.logger.AppenderConstants#PARAM_LOCATION_LABEL}'</li>
 * <li>Tag - resolved set of values {host name, application name} for RFC 3164 and set of values {facility name, host
 * name, application name, message id} for RFC 5424, or application message contained variable named
 * '{@value com.jkoolcloud.tnt4j.logger.AppenderConstants#PARAM_TAG_LABEL}'</li>
 * <li>Correlator - resolved from log event application message contained variable named
 * '{@value com.jkoolcloud.tnt4j.logger.AppenderConstants#PARAM_CORRELATOR_LABEL}'</li>
 * <li>ProcessId - resolved log event process id</li>
 * <li>ThreadId - same as 'ProcessId'</li>
 * <li>Message - resolved log event application message</li>
 * <li>Severity - resolved log event level mapped to {@link OpLevel}</li>
 * <li>ApplName - resolved log event application name</li>
 * <li>ServerName - resolved log event host name</li>
 * <li>EndTime - resolved log event timestamp value in microseconds</li>
 * <li>ElapsedTime - calculated time difference between same host and app events in microseconds</li>
 * <li>MsgCharSet - resolved log event char set name</li>
 * </ul>
 * <li>for activity properties:</li>
 * <ul>
 * <li>facility - resolved log event facility name</li>
 * <li>level - resolved log event level</li>
 * <li>hostname - resolved log event host name</li>
 * <li>hostaddr - resolved log event host address</li>
 * </ul>
 * <li>maps of resolved additional custom activity properties:</li>
 * <ul>
 * <li>SyslogMap - map of resolved RFC 5424 structured data</li>
 * <li>SyslogVars - map of resolved application message contained (varName=varValue) variables</li>
 * </ul>
 * </ul>
 * <p>
 * This activity parser supports properties from {@link AbstractActivityMapParser} (and higher hierarchy parsers).
 *
 * @version $Revision: 1 $
 */
public class ActivitySyslogEventParser extends AbstractActivityMapParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivitySyslogEventParser.class);

	private static final String ATTR_SERVER_NAME = "server.name"; // NON-NLS
	private static final String ATTR_APPL_PART = "appl.part"; // NON-NLS
	private static final String ATTR_APPL_NAME = "appl.name"; // NON-NLS
	private static final String ATTR_APPL_PID = "appl.pid"; // NON-NLS

	/*
	 * Timing map maintains the number of nanoseconds since last event for a specific server/application combo.
	 */
	private static final TimeTracker TIME_TRACKER = TimeTracker.newTracker(10000, TimeUnit.DAYS.toMillis(30));

	/**
	 * Constructs a new ActivitySyslogEventParser.
	 */
	public ActivitySyslogEventParser() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link org.graylog2.syslog4j.server.SyslogServerEventIF}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return SyslogServerEventIF.class.isInstance(data);
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		super.setProperties(props);

		// for (Map.Entry<String, String> prop : props) {
		// String name = prop.getKey();
		// String value = prop.getValue();
		//
		// // no any additional properties are required yet.
		// if (false) {
		// logger().log(OpLevel.DEBUG,
		// StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
		// name, value);
		// }
		// }
	}

	@Override
	protected Map<String, ?> getDataMap(Object data) {
		if (data == null) {
			return null;
		}

		SyslogServerEventIF event = (SyslogServerEventIF) data;

		Map<String, Object> dataMap = new HashMap<>();

		Date date = (event.getDate() == null ? new Date() : event.getDate());
		String facility = SyslogUtils.getFacilityString(event.getFacility());
		OpLevel level = SyslogUtils.getOpLevel(event.getLevel());

		dataMap.put(EventName.name(), facility);
		dataMap.put(Message.name(), event.getMessage());
		dataMap.put(Severity.name(), level);
		dataMap.put(FIELD_FACILITY, facility);
		dataMap.put(FIELD_HOSTNAME, event.getHost());
		dataMap.put(FIELD_LEVEL, event.getLevel());

		InetSocketAddress from = null;
		if (event instanceof SyslogServerEvent) {
			try {
				Field addressField = event.getClass().getField("inetAddress"); // NON-NLS
				addressField.setAccessible(true);
				from = (InetSocketAddress) addressField.get(event);
			} catch (Exception exc) {
			}
		}
		if (from == null) {
			dataMap.put(Location.name(), event.getHost());
		} else {
			dataMap.put(FIELD_HOSTADDR, from.getAddress().getHostAddress());
			dataMap.put(Location.name(), from.getAddress().getHostAddress());
		}
		dataMap.put(MsgCharSet.name(), event.getCharSet());

		if (event instanceof StructuredSyslogServerEvent) {
			processRFC5424(facility, (StructuredSyslogServerEvent) event, dataMap);
		} else {
			processRFC3164(facility, event, dataMap);
		}

		// extract name=value pairs if available
		SyslogUtils.extractVariables(event.getMessage(), dataMap);
		String eventKey = String.format("%s/%s", dataMap.get(Location.name()), dataMap.get(ResourceName.name())); // NON-NLS
		dataMap.put(EndTime.name(), date.getTime() * 1000);
		dataMap.put(ElapsedTime.name(), getUsecSinceLastEvent(eventKey));

		return dataMap;
	}

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - Syslog event
	 */
	@Override
	protected String getActivityDataType() {
		return "SYSLOG EVENT"; // NON-NLS
	}

	/**
	 * Process Syslog message based on RFC 5424.
	 *
	 * @param facility
	 *            Syslog facility name
	 * @param event
	 *            Syslog message
	 * @param dataMap
	 *            log entry fields map to update
	 */
	protected static void processRFC3164(String facility, SyslogServerEventIF event, Map<String, Object> dataMap) {
		Map<String, Object> map = parseAttributes(event);
		String serverName = map.get(ATTR_SERVER_NAME).toString();
		String appName = map.get(ATTR_APPL_NAME).toString();
		Long pid = (Long) map.get(ATTR_APPL_PID);

		dataMap.put(Tag.name(), new String[] { serverName, appName });
		dataMap.put(ProcessId.name(), pid);
		dataMap.put(ThreadId.name(), pid);
		dataMap.put(ResourceName.name(), appName);
		dataMap.put(EventName.name(), facility);

		// set the appropriate source
		dataMap.put(ApplName.name(), appName);
		dataMap.put(ServerName.name(), serverName);
	}

	/**
	 * Process Syslog message based on RFC 5424.
	 *
	 * @param facility
	 *            Syslog facility name
	 * @param sEvent
	 *            Syslog structured message
	 * @param dataMap
	 *            log entry fields map to update
	 */
	protected static void processRFC5424(String facility, StructuredSyslogServerEvent sEvent,
			Map<String, Object> dataMap) {
		// RFC 5424
		StructuredSyslogMessage sMessage = sEvent.getStructuredMessage();
		String msgId = sMessage.getMessageId();
		dataMap.put(EventName.name(), facility);
		dataMap.put(ResourceName.name(), sEvent.getApplicationName());
		dataMap.put(Tag.name(), new String[] { facility, sEvent.getHost(), sEvent.getApplicationName(), msgId });
		// if (StringUtils.isNotEmpty(msgId)) {
		// dataMap.put(TrackingId.name(), msgId);
		// }
		SyslogUtils.assignPid(sEvent.getProcessId(), dataMap);

		// set the appropriate source
		dataMap.put(ApplName.name(), sEvent.getApplicationName());
		dataMap.put(ServerName.name(), sEvent.getHost());

		// process structured event attributes into snapshot
		extractStructuredData(sEvent, dataMap);
	}

	/**
	 * Extract Syslog structured data if available (part of RFC 5424).
	 *
	 * @param sEvent
	 *            Syslog structured message
	 * @param dataMap
	 *            log entry fields map to update
	 * @return map of structured Syslog message data
	 */
	protected static Map<String, Map<String, String>> extractStructuredData(StructuredSyslogServerEvent sEvent,
			Map<String, Object> dataMap) {
		StructuredSyslogMessage sm = sEvent.getStructuredMessage();
		Map<String, Map<String, String>> map = sm.getStructuredData();
		if (MapUtils.isNotEmpty(map)) {
			// PropertySnapshot snap = new PropertySnapshot(FIELD_SYSLOG_MAP,
			// sEvent.getApplicationName(),
			// (OpLevel) dataMap.get(Severity.name()));
			// snap.addAll(map);
			dataMap.put(FIELD_SYSLOG_MAP, map);
		}
		return map;
	}

	/**
	 * Parse Syslog header attributes into a map. Message structure: <server> <appl-part>:<message>
	 *
	 * @param event
	 *            Syslog event
	 * @return Syslog attributes such as host, application, pid
	 */
	private static Map<String, Object> parseAttributes(SyslogServerEventIF event) {
		HashMap<String, Object> map = new HashMap<>();
		String message = event.getMessage();

		if (message != null && message.indexOf(COLON) > 0) {
			String[] tokens = message.split(":| "); // NON-NLS
			map.put(ATTR_SERVER_NAME, tokens[0]);
			map.put(ATTR_APPL_PART, tokens[1]);

			try {
				int first = tokens[1].indexOf(OB);
				int last = tokens[1].indexOf(CB);
				String applName = first >= 0 ? tokens[1].substring(0, first) : tokens[1];
				long pid = ((last >= first && (first >= 0)) ? Long.parseLong(tokens[1].substring(first + 1, last)) : 0);
				map.put(ATTR_APPL_NAME, applName);
				map.put(ATTR_APPL_PID, pid);
			} catch (Throwable ex) {
				map.put(ATTR_APPL_NAME, tokens[1]);
				map.put(ATTR_APPL_PID, 0L);
			}
		} else {
			map.put(ATTR_SERVER_NAME, event.getHost());
			map.put(ATTR_APPL_NAME, UNKNOWN);
			map.put(ATTR_APPL_PID, 0L);
		}
		return map;
	}

	/**
	 * Obtain elapsed microseconds since last event.
	 *
	 * @param key
	 *            timer key
	 * @return elapsed microseconds since last event
	 */
	public static long getUsecSinceLastEvent(String key) {
		return TimeUnit.NANOSECONDS.toMicros(TIME_TRACKER.hitAndGet(key));
	}
}
