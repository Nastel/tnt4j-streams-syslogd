/*
 * Copyright (C) 2015-2016, JKOOL LLC.
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

package com.jkoolcloud.tnt4j.streams.utils;

import static com.jkoolcloud.tnt4j.streams.fields.StreamFieldType.*;
import static com.jkoolcloud.tnt4j.streams.fields.StreamFieldType.Exception;
import static com.jkoolcloud.tnt4j.streams.utils.SyslogStreamConstants.LEVELS;

import java.lang.Exception;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.OpType;
import com.jkoolcloud.tnt4j.logger.AppenderConstants;
import com.jkoolcloud.tnt4j.tracker.TimeTracker;

/**
 * Utility methods used by Syslog module.
 *
 * @version $Revision: 1 $
 */
public final class SyslogUtils {

	/*
	 * Regular expression pattern to detect name=value pairs.
	 */
	private static final Pattern VARIABLES_PATTERN = Pattern.compile("(\\S+)=\"*((?<=\")[^\"]+(?=\")|([^\\s]+))\"*");

	/*
	 * Timing map maintains the number of nanoseconds since last event for a specific server/application combo.
	 */
	private static final TimeTracker TIME_TRACKER = TimeTracker.newTracker(10000, TimeUnit.DAYS.toMillis(30));

	private SyslogUtils() {

	}

	/**
	 * Extract syslog name/value pairs if available in within the message
	 *
	 * @param message
	 *            syslog event message
	 * @param dataMap
	 *            log entry fields map to update
	 * @return map of parsed out event attributes (name=value pairs)
	 */
	public static Map<String, Object> extractVariables(String message, Map<String, Object> dataMap) {
		if (message == null) {
			return null;
		}
		Map<String, Object> vars = parseVariables(message);
		if (MapUtils.isNotEmpty(vars)) {
			extractSpecialKeys(vars, dataMap);
			// PropertySnapshot snap = new PropertySnapshot(FIELD_SYSLOG_VARS,
			// (String) dataMap.get(ResourceName.name()), (OpLevel) dataMap.get(Severity.name()));
			// snap.addAll(vars);
			dataMap.put(SyslogStreamConstants.FIELD_SYSLOG_VARS, vars);
		}
		return vars;
	}

	/**
	 * Parse syslog name=value variables
	 *
	 * @param message
	 *            syslog message
	 * @return syslog name=value variables
	 */
	public static Map<String, Object> parseVariables(String message) {
		Map<String, Object> map = new HashMap<String, Object>();
		StringTokenizer tokens = new StringTokenizer(message, "[](){}"); // NON-NLS

		while (tokens.hasMoreTokens()) {
			String pair = tokens.nextToken();
			Matcher matcher = VARIABLES_PATTERN.matcher(pair);
			while (matcher.find()) {
				mapToTyped(map, matcher.group(1), matcher.group(2));
			}
		}
		return map;
	}

	/**
	 * Test key value pair for numeric, convert and store in map
	 *
	 * @param map
	 *            collection of name, value pairs
	 * @param key
	 *            associated with key, value pair
	 * @param value
	 *            associated with key, value pair
	 */
	private static void mapToTyped(Map<String, Object> map, String key, String value) {
		if (Character.isDigit(value.charAt(0))) {
			Number num = null;
			try {
				num = Long.valueOf(value);
			} catch (Exception el) {
				try {
					num = Double.valueOf(value);
				} catch (Exception ed) {
				}
			}

			map.put(key, num == null ? value : num);
		}

		map.put(key, value);
	}

	/**
	 * Process a given map of key/value pairs into a TNT4J event object
	 * {@link com.jkoolcloud.tnt4j.tracker.TrackingEvent}.
	 *
	 * @param attrs
	 *            a set of name/value pairs
	 * @param dataMap
	 *            log entry fields map to update
	 */
	private static void extractSpecialKeys(Map<String, Object> attrs, Map<String, Object> dataMap) {
		for (Map.Entry<String, Object> entry : attrs.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue().toString();
			if (key.equals(AppenderConstants.PARAM_CORRELATOR_LABEL)) {
				dataMap.put(Correlator.name(), value);
			} else if (key.equals(AppenderConstants.PARAM_TAG_LABEL)) {
				dataMap.put(Tag.name(), value);
			} else if (key.equals(AppenderConstants.PARAM_LOCATION_LABEL)) {
				dataMap.put(Location.name(), value);
			} else if (key.equals(AppenderConstants.PARAM_RESOURCE_LABEL)) {
				dataMap.put(ResourceName.name(), value);
			} else if (key.equals(AppenderConstants.PARAM_USER_LABEL)) {
				dataMap.put(UserName.name(), value);
			} else if (key.equals(AppenderConstants.PARAM_OP_TYPE_LABEL)) {
				dataMap.put(EventType.name(), OpType.valueOf(value));
			} else if (key.equals(AppenderConstants.PARAM_OP_NAME_LABEL)) {
				dataMap.put(EventName.name(), value);
			} else if (key.equals(AppenderConstants.PARAM_EXCEPTION_LABEL)) {
				dataMap.put(Exception.name(), value);
			}
		}
	}

	/**
	 * Extract and assign process id
	 *
	 * @param pid
	 *            process identifier
	 * @param dataMap
	 *            log entry fields map to update
	 */
	public static void assignPid(String pid, Map<String, Object> dataMap) {
		dataMap.put(ProcessId.name(), 0);
		if (StringUtils.isNotEmpty(pid)) {
			try {
				dataMap.put(ProcessId.name(), Long.parseLong(pid));
			} catch (NumberFormatException e) {
			}
		}
		dataMap.put(ThreadId.name(), dataMap.get(ProcessId.name()));
	}

	/**
	 * Obtain string representation of syslog facility
	 *
	 * @param facility
	 *            syslog facility
	 * @return string representation of syslog facility
	 */
	public static String getFacilityString(int facility) {
		return SyslogStreamConstants.Facility.valueOf(facility).name();
	}

	/**
	 * Obtain syslog level to {@link OpLevel} mapping
	 *
	 * @param level
	 *            syslog level
	 * @return {@link OpLevel} mapping
	 */
	public static OpLevel getOpLevel(int level) {
		return ((level >= 0) && (level < LEVELS.length)) ? LEVELS[level] : LEVELS[LEVELS.length - 1];
	}

	/**
	 * Obtain elapsed microseconds since last event
	 *
	 * @param key
	 *            timer key
	 * @return elapsed microseconds since last event
	 */
	public static long getUsecSinceLastEvent(String key) {
		return TimeUnit.NANOSECONDS.toMicros(TIME_TRACKER.hitAndGet(key));
	}
}
