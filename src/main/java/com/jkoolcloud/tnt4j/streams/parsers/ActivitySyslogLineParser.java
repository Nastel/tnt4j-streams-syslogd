/*
 * Copyright (C) 2015-2018, JKOOL LLC.
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

import java.io.IOException;
import java.lang.Exception;
import java.nio.CharBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.SyslogParserProperties;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Implements an activity data parser that assumes each activity data item is an Syslog log line {@link String}. Parser
 * resolved log line fields are put into {@link Map} and afterwards mapped into activity fields and properties according
 * to defined parser configuration.
 * <p>
 * Map entries containing values as internal {@link Map}s are automatically mapped into activity properties. If only
 * particular inner map entries are needed, then in parser fields mapping configuration define those properties as
 * separate fields.
 * <p>
 * This parser resolved data map may contain such entries:
 * <ul>
 * <li>for activity fields:</li>
 * <ul>
 * <li>EventType - resolved from log line application message contained variable named
 * {@value com.jkoolcloud.tnt4j.logger.AppenderConstants#PARAM_OP_TYPE_LABEL}</li>
 * <li>EventName - resolved log line facility name, or application message contained variable named
 * {@value com.jkoolcloud.tnt4j.logger.AppenderConstants#PARAM_OP_NAME_LABEL}</li>
 * <li>Exception - resolved from log line application message contained variable named
 * {@value com.jkoolcloud.tnt4j.logger.AppenderConstants#PARAM_EXCEPTION_LABEL}</li>
 * <li>UserName - resolved from log line application message contained variable named
 * {@value com.jkoolcloud.tnt4j.logger.AppenderConstants#PARAM_USER_LABEL}</li>
 * <li>ResourceName - resolved log line application name, or application message contained variable named
 * {@value com.jkoolcloud.tnt4j.logger.AppenderConstants#PARAM_RESOURCE_LABEL}</li>
 * <li>Location - resolved log line host name/address, or application message contained variable named
 * {@value com.jkoolcloud.tnt4j.logger.AppenderConstants#PARAM_LOCATION_LABEL}</li>
 * <li>Tag - resolved set of values {host name, application name} for RFC 3164 and set of values {facility name, host
 * name, application name, message id} for RFC 5424, or application message contained variable named
 * {@value com.jkoolcloud.tnt4j.logger.AppenderConstants#PARAM_TAG_LABEL}</li>
 * <li>Correlator - resolved from log line application message contained variable named
 * {@value com.jkoolcloud.tnt4j.logger.AppenderConstants#PARAM_CORRELATOR_LABEL}</li>
 * <li>ProcessId - resolved log line process id</li>
 * <li>ThreadId - same as 'ProcessId'</li>
 * <li>Message - resolved log line application message</li>
 * <li>Severity - resolved log line level mapped to {@link OpLevel}</li>
 * <li>ApplName - resolved log line application name</li>
 * <li>ServerName - resolved log line host name</li>
 * <li>EndTime - resolved log line timestamp value in microseconds</li>
 * <li>ElapsedTime - calculated time difference between same host and app events in microseconds</li>
 * <li>MsgCharSet - char set name used by parser</li>
 * </ul>
 * <li>for activity properties:</li>
 * <ul>
 * <li>facility - resolved log line facility name. If resolved 'priority' is {@code null} - then value is
 * '{@link com.jkoolcloud.tnt4j.streams.utils.SyslogStreamConstants.Facility#USER}'</li>
 * <li>level - resolved log line level. If resolved 'priority' is {@code null} - then value is
 * {@value com.jkoolcloud.tnt4j.streams.utils.SyslogStreamConstants#DEFAULT_LEVEL}</li>
 * <li>hostname - resolved log line host name</li>
 * <li>version - resolved log line Syslog version ('0' for RFC 3164, '1' for RFC 5424)</li>
 * <li>priority - resolved log line priority</li>
 * </ul>
 * <li>maps of resolved additional custom activity properties:</li>
 * <ul>
 * <li>SyslogMap - map of resolved RFC 5424 structured data</li>
 * <li>SyslogVars - map of resolved application message contained (varName=varValue) variables</li>
 * </ul>
 * </ul>
 * <p>
 * This parser supports the following properties (in addition to those supported by {@link AbstractSyslogParser}):
 * <ul>
 * <li>CharSet - name of char set used by Syslog lines parser. Default value - 'UTF-8'. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivitySyslogLineParser extends AbstractSyslogParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivitySyslogLineParser.class);

	private static final String DEFAULT_CHAR_SET = Utils.UTF8;

	private String streamCharSet = DEFAULT_CHAR_SET;

	private final SyslogParser syslogParser;

	/**
	 * Constructs a new ActivitySyslogLineParser.
	 */
	public ActivitySyslogLineParser() {
		super();

		syslogParser = new SyslogParser();
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
	 * <li>{@link java.lang.String}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return String.class.isInstance(data);
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		super.setProperties(props);

		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				String name = prop.getKey();
				String value = prop.getValue();

				if (SyslogParserProperties.PROP_CHAR_SET.equalsIgnoreCase(name)) {
					streamCharSet = value;

					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"ActivityParser.setting", name, value);
				}
			}
		}
	}

	@Override
	protected Map<String, Object> getDataMap(Object data) {
		if (data == null) {
			return null;
		}

		String msg = (String) data;

		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put(RAW_ACTIVITY_STRING_KEY, msg);

		try {
			synchronized (syslogParser) {
				dataMap.putAll(syslogParser.parse(msg));
			}
		} catch (Exception exc) {
			Utils.logThrowable(logger(), OpLevel.ERROR,
					StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
					"ActivitySyslogLineParser.line.parse.failed", exc);
		}

		return suppress(dataMap);
	}

	// @Override
	// protected ActivityInfo parsePreparedItem(TNTInputStream<?, ?> stream, String dataStr, Map<String, ?> data)
	// throws ParseException {
	// if (data == null) {
	// return null;
	// }
	//
	// ActivityInfo ai = new ActivityInfo();
	// ActivityField field = null;
	//
	// for (Map.Entry<String, ?> dme : data.entrySet()) {
	// applyFieldValue(stream, ai, null, dme.getValue());
	// }
	//
	// return ai;
	// }

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - Syslog line
	 */
	@Override
	protected String getActivityDataType() {
		return "SYSLOG LINE"; // NON-NLS
	}

	/**
	 * Syslog log lines parser for RFC 3164 and 5424.
	 */
	private class SyslogParser extends CharBufferParser<String, Map<String, ?>> {
		private static final String SYSLOG_STRUCT_ID = "struct.id"; // NON-NLS

		// As defined in RFC 5424.
		private static final int MAX_SUPPORTED_VERSION = 1;

		private final Map<String, Long> EVENT_TIMESTAMP_MAP = new HashMap<>(8);

		/**
		 * Construct a new Syslog log lines parser.
		 */
		public SyslogParser() {
			super();
		}

		/**
		 * Parse Syslog log line string making map of parsed fields.
		 *
		 * @param logLine
		 *            Syslog log line
		 * @return a map, or {@code null} if line is empty.
		 *
		 * @throws IOException
		 *             if the underlying stream fails, or unexpected chars occurs.
		 */
		@Override
		public Map<String, Object> parse(String logLine) throws IOException {
			CharBuffer cb = stringToBuffer(logLine);

			Integer priority = null;
			int c = read(cb);

			if (c == EOF) {
				return null;
			}

			if (c == LT) {
				priority = readInt(cb);

				expect(cb, GT);
			} else {
				unread(cb);
			}

			int version = 0;
			Calendar cal;

			if (Character.isDigit(peek(cb))) {
				// Assume ISO date and time
				int y = readInt(cb);

				c = read(cb);

				if (c == SPACE) {
					// Assume this is a RFC 5424 message.
					version = y;

					if (version > MAX_SUPPORTED_VERSION) {
						throw new IOException(
								StreamsResources.getStringFormatted(SyslogStreamConstants.RESOURCE_BUNDLE_NAME,
										"ActivitySyslogLineParser.unsupported.version", version));
					}

					skipSpaces(cb);
					y = readInt(cb);
					expect(cb, MINUS);
				} else if (c != MINUS) {
					throw new IOException(
							StreamsResources.getStringFormatted(SyslogStreamConstants.RESOURCE_BUNDLE_NAME,
									"ActivitySyslogLineParser.unexpected.char", MINUS, (char) c));
				}

				int m = readInt(cb);
				expect(cb, MINUS);
				int d = readInt(cb);

				c = read(cb);

				if (c != TZ && c != SPACE) {
					throw new IOException(
							StreamsResources.getStringFormatted(SyslogStreamConstants.RESOURCE_BUNDLE_NAME,
									"ActivitySyslogLineParser.unexpected.char", TZ, (char) c));
				}

				int hh = readInt(cb);
				expect(cb, COLON);
				int mm = readInt(cb);
				expect(cb, COLON);
				int ss = readInt(cb);
				double sf = 0;

				c = read(cb);

				if (c == DOT) {
					// Fractions of seconds
					sf = readFractions(cb);
					c = read(cb);
				}

				int tz = 0;

				if (c == UTC) {
					// UTC time zone found
				} else if (c == MINUS) {
					tz = readInt(cb);

					if (peek(cb) == COLON) {
						read(cb);
						tz = -(tz * 60 + readInt(cb));
					}
				} else if (c == PLUS) {
					tz = readInt(cb);

					if (peek(cb) == COLON) {
						read(cb);
						tz = tz * 60 + readInt(cb);
					}
				}

				cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.getDefault());

				cal.set(y, m - 1, d, hh, mm, ss);
				cal.set(Calendar.MILLISECOND, (int) (sf * 1000));
				cal.add(Calendar.MINUTE, tz);
			} else {
				// Assume BSD date and time
				int m = readMonthAbbreviation(cb);

				expect(cb, SPACE);
				skipSpaces(cb);

				int d = readInt(cb);

				expect(cb, SPACE);
				skipSpaces(cb);

				int hh = readInt(cb);

				expect(cb, COLON);

				int mm = readInt(cb);

				expect(cb, COLON);

				int ss = readInt(cb);

				cal = new GregorianCalendar(Locale.ROOT);

				cal.set(Calendar.MONTH, m);
				cal.set(Calendar.DAY_OF_MONTH, d);
				cal.set(Calendar.HOUR_OF_DAY, hh);
				cal.set(Calendar.MINUTE, mm);
				cal.set(Calendar.SECOND, ss);
				cal.set(Calendar.MILLISECOND, 0);
			}

			expect(cb, SPACE);
			skipSpaces(cb);

			String hostName = readWord(cb, 32);

			expect(cb, SPACE);

			String appName = null;
			String procId = null;
			String msgId = null;
			Map<String, Object> structuredData = null;

			if (version >= 1) {
				appName = readWordOrNil(cb, 20);
				expect(cb, SPACE);
				procId = readWordOrNil(cb, 20);
				expect(cb, SPACE);
				msgId = readWordOrNil(cb, 20);
				expect(cb, SPACE);
				structuredData = readStructuredData(cb);
				expectOrEnd(cb, SPACE);
			} else if (version == 0) {
				// Try to find a colon terminated tag.
				appName = readTag(cb);
				if (peek(cb) == OB) {
					procId = readPid(cb);
				}
				expect(cb, COLON);
			}

			appName = StringUtils.isEmpty(appName) ? UNKNOWN : appName;

			skipSpaces(cb);

			String appMsg = readLine(cb, 128);

			return createFieldMap(version, priority, cal, hostName, appName, procId, msgId, structuredData, appMsg);
		}

		/**
		 * Create a map from the parsed fields data.
		 *
		 * @param version
		 *            the resolved Syslog version: 0 for RFC 3164
		 * @param priority
		 *            the resolved Syslog priority according to RFC 5424
		 * @param date
		 *            the resolved timestamp with timezone
		 * @param hostName
		 *            the resolved host name
		 * @param appName
		 *            the resolved application name
		 * @param procId
		 *            the resolved process id
		 * @param msgId
		 *            the resolved message id according to RFC 5424
		 * @param structuredData
		 *            the resolved structured data map according to RFC 5424
		 * @param appMsg
		 *            the resolved application message
		 */
		private Map<String, Object> createFieldMap(int version, Integer priority, Calendar date, String hostName,
				String appName, String procId, String msgId, Map<String, Object> structuredData, String appMsg) {
			Map<String, Object> map = new HashMap<>(16);
			int facility = priority == null ? DEFAULT_FACILITY.ordinal() : priority / 8;
			int level = priority == null ? DEFAULT_LEVEL : priority % 8;
			String facilityStr = SyslogUtils.getFacilityString(facility);

			map.put(EventName.name(), facilityStr);
			map.put(ResourceName.name(), appName);
			map.put(Location.name(), hostName);
			if (version >= 1) {
				map.put(Tag.name(), new String[] { facilityStr, hostName, appName, msgId });
			} else {
				map.put(Tag.name(), new String[] { hostName, appName });
			}

			map.put(FIELD_FACILITY, facilityStr);
			map.put(Severity.name(), SyslogUtils.getOpLevel(level));
			map.put(FIELD_LEVEL, level);
			map.put(FIELD_VERSION, version);
			SyslogUtils.assignPid(procId, map);
			// if (StringUtils.isNotEmpty(msgId)) {
			// map.put(TrackingId.name(), msgId);
			// }
			if (structuredData != null) {
				map.put(FIELD_SYSLOG_MAP, structuredData);
			}

			map.put(Message.name(), appMsg);
			if (priority != null) {
				map.put(FIELD_PRIORITY, priority);
			}
			map.put(FIELD_HOSTNAME, hostName);
			map.put(MsgCharSet.name(), streamCharSet);

			// set the appropriate source
			map.put(ApplName.name(), appName);
			map.put(ServerName.name(), hostName);

			// extract name=value pairs if available
			SyslogUtils.extractVariables(appMsg, map);
			String eventKey = String.format("%s/%s", (String) map.get(Location.name()), // NON-NLS
					(String) map.get(ResourceName.name()));
			long eventTime = date.getTimeInMillis();
			map.put(EndTime.name(), eventTime * 1000);
			map.put(ElapsedTime.name(), getUsecSinceLastEvent(eventKey, eventTime));

			return map;
		}

		/**
		 * Obtain elapsed microseconds since last log event.
		 *
		 * @param eventKey
		 *            event key
		 * @param eventTime
		 *            current event timestamp value
		 * @return elapsed microseconds since last event
		 */
		private long getUsecSinceLastEvent(String eventKey, long eventTime) {
			synchronized (EVENT_TIMESTAMP_MAP) {
				Long prev_ts = EVENT_TIMESTAMP_MAP.put(eventKey, eventTime);

				if (prev_ts == null) {
					prev_ts = eventTime;
				}

				return TimeUnit.MILLISECONDS.toMicros(eventTime - prev_ts);
			}
		}

		/**
		 * Read a month value as an English abbreviation. See RFC 3164 sec. 4.1.2.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 * @return resolved month index, or {@code -1} if unknown
		 */
		private int readMonthAbbreviation(CharBuffer cb) {
			String mStr = readChars(cb, 3);

			return SyslogUtils.getMonthIndex(mStr);
		}

		/**
		 * Read a word from buffer as a string. Word is terminated by whitespace char (' ') or end of buffer. If the
		 * complete word is "-", returns {@code null}.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 * @param sizeHint
		 *            an guess on how large string will be, in chars
		 * @return word string read from buffer
		 *
		 * @see #readWord(CharBuffer, int)
		 */
		private String readWordOrNil(CharBuffer cb, int sizeHint) {
			String ret = readWord(cb, sizeHint);

			if (ret.length() == 1 && ret.charAt(0) == MINUS) {
				return null;
			}

			return ret;
		}

		/**
		 * Read a RFC 3164 tag. Tags is terminated by one of: ':[\r\n'.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 * @return resolved tag string
		 *
		 * @see #read(CharBuffer)
		 * @see #unread(CharBuffer)
		 * @see StringBuilder#append(char)
		 */
		private String readTag(CharBuffer cb) {
			StringBuilder sb = new StringBuilder(16);
			int c;

			while ((c = read(cb)) != COLON && c != OB && c != RC && c != NL) {
				sb.append((char) c);
			}

			unread(cb);

			return sb.toString();
		}

		/**
		 * Read a RFC 3164 pid. Pid format is: '[1234]'.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 * @return resolved process id string
		 *
		 * @throws IOException
		 *             if unexpected character is found while parsing
		 *
		 * @see #read(CharBuffer)
		 * @see #expect(CharBuffer, int)
		 * @see StringBuilder#append(char)
		 */
		private String readPid(CharBuffer cb) throws IOException {
			StringBuilder sb = new StringBuilder(8);
			int c;

			expect(cb, OB);

			while ((c = read(cb)) != CB && c != RC && c != NL) {
				sb.append((char) c);
			}

			return sb.toString();
		}

		/**
		 * Read RFC 5424 structured data map.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 * @return map of resolved structured data
		 *
		 * @throws IOException
		 *             if unexpected character is found while parsing
		 */
		private Map<String, Object> readStructuredData(CharBuffer cb) throws IOException {
			int c = read(cb);

			if (c == MINUS) {
				return null;
			}

			if (c != OB) {
				throw new IOException(StreamsResources.getStringFormatted(SyslogStreamConstants.RESOURCE_BUNDLE_NAME,
						"ActivitySyslogLineParser.unexpected.char", OB, (char) c));
			}

			Map<String, Object> sdm = new HashMap<>();
			StringBuilder sb = new StringBuilder();

			while (c == OB) {
				// Read structured data id
				while ((c = read(cb)) != SPACE && c != CB) {
					sb.append((char) c);
				}
				sdm.put(SYSLOG_STRUCT_ID, sb.toString());
				sb.setLength(0);

				String paramName;
				while (c == SPACE) {
					// Read parameter name
					while ((c = read(cb)) != EQ) {
						sb.append((char) c);
					}
					paramName = sb.toString();
					sb.setLength(0);

					expect(cb, QUOTE);

					// Read parameter data
					while ((c = read(cb)) != QUOTE) {
						sb.append((char) c);

						if (c == SyslogStreamConstants.SLASH) {
							c = read(cb);
							sb.append((char) c);
						}
					}
					sdm.put(paramName, sb.toString());
					sb.setLength(0);

					c = read(cb);
				}

				if (c != CB) {
					throw new IOException(
							StreamsResources.getStringFormatted(SyslogStreamConstants.RESOURCE_BUNDLE_NAME,
									"ActivitySyslogLineParser.unexpected.char", CB, (char) c));
				}

				c = read(cb);
			}

			unread(cb);

			return sdm;
		}
	}
}
