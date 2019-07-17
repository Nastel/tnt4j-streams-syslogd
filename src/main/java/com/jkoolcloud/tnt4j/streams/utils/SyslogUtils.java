/*
 * Copyright (C) 2015-2019, JKOOL LLC.
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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.syslog4j.util.SyslogUtility;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.OpType;
import com.jkoolcloud.tnt4j.logger.AppenderConstants;
import com.jkoolcloud.tnt4j.sink.EventSink;

/**
 * Utility methods used by Syslog module.
 *
 * @version $Revision: 1 $
 */
public final class SyslogUtils {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(SyslogUtils.class);

	/*
	 * Regular expression pattern to detect {@code name=value}, {@code name="value"}, {@code name(value)} pairs.
	 */
	private static final Pattern VARIABLES_PATTERN = Pattern.compile(
			"(?<key>[^=,\\s(]+)\\s*(?:(?:=\\s*[\"(](?<value1>[^\")]*)[\")])|(?:=\\s*(?<value2>.*?)[,\\s]+)|(?:\\((?<value3>.*?)\\)))");

	private SyslogUtils() {
	}

	/**
	 * Extract Syslog name/value pairs if available in within the message.
	 *
	 * @param message
	 *            Syslog event message
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
	 * Parse Syslog variable key/value pairs having format {@code name=value} or {@code name(value)}.
	 *
	 * @param message
	 *            Syslog message
	 * @return resolved Syslog variables key/value pairs map
	 */
	public static Map<String, Object> parseVariables(String message) {
		Map<String, Object> map = new HashMap<>();

		parserVariablePairs(message, VARIABLES_PATTERN, map);

		return map;
	}

	private static void parserVariablePairs(String str, Pattern pattern, Map<String, Object> map) {
		Matcher matcher = pattern.matcher(str);
		while (matcher.find()) {
			String key = matcher.group("key"); // NON-NLS
			if (StringUtils.isEmpty(key)) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
						"SyslogUtils.variable.key.empty", matcher.group(0));
				continue;
			}

			String val = matcher.group("value1"); // NON-NLS
			if (val == null) {
				val = matcher.group("value2"); // NON-NLS
			}
			if (val == null) {
				val = matcher.group("value3"); // NON-NLS
			}

			mapToTyped(map, key.trim(), val);
		}
	}

	/**
	 * Test key value pair for numeric, convert and store in map.
	 *
	 * @param map
	 *            collection of name, value pairs
	 * @param key
	 *            associated with key, value pair
	 * @param value
	 *            associated with key, value pair
	 */
	private static void mapToTyped(Map<String, Object> map, String key, String value) {
		Number num = null;
		if (StringUtils.isNotEmpty(value) && Character.isDigit(value.charAt(0))) {
			try {
				num = Long.valueOf(value);
			} catch (Exception el) {
				try {
					num = Double.valueOf(value);
				} catch (Exception ed) {
				}
			}
		}

		map.put(key, num == null ? value : num);
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
	 * Extract and assign process id.
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
	 * Obtain string representation of Syslog facility.
	 *
	 * @param facility
	 *            Syslog facility
	 * @return string representation of Syslog facility
	 */
	public static String getFacilityString(int facility) {
		return SyslogUtility.getFacilityString(facility);
	}

	/**
	 * Obtain Syslog level to {@link OpLevel} mapping.
	 *
	 * @param level
	 *            Syslog level
	 * @return {@link OpLevel} mapping
	 */
	public static OpLevel getOpLevel(int level) {
		return ((level >= 0) && (level < LEVELS.length)) ? LEVELS[level] : LEVELS[LEVELS.length - 1];
	}

	private static final Map<String, Integer> MONTH_MAP;
	static {
		Map<String, Integer> mMap = new HashMap<>(12);
		mMap.put("jan", Calendar.JANUARY); // NON-NLS
		mMap.put("feb", Calendar.FEBRUARY); // NON-NLS
		mMap.put("mar", Calendar.MARCH); // NON-NLS
		mMap.put("apr", Calendar.APRIL); // NON-NLS
		mMap.put("may", Calendar.MAY); // NON-NLS
		mMap.put("jun", Calendar.JUNE); // NON-NLS
		mMap.put("jul", Calendar.JULY); // NON-NLS
		mMap.put("aug", Calendar.AUGUST); // NON-NLS
		mMap.put("sep", Calendar.SEPTEMBER); // NON-NLS
		mMap.put("oct", Calendar.OCTOBER); // NON-NLS
		mMap.put("nov", Calendar.NOVEMBER); // NON-NLS
		mMap.put("dec", Calendar.DECEMBER); // NON-NLS
		MONTH_MAP = Collections.unmodifiableMap(mMap);
	}

	/**
	 * Resolves month name 3 letter abbreviation to {@link Calendar} month index.
	 *
	 * @param mName
	 *            month name 3 letter abbreviation
	 * @return month index, or {@code -1} if unknown or empty
	 */
	public static Integer getMonthIndex(String mName) {
		Integer mIdx = null;

		if (StringUtils.isNotEmpty(mName)) {
			mIdx = MONTH_MAP.get(mName.toLowerCase());
		}

		return mIdx == null ? -1 : mIdx;
	}

}
