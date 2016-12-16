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

package com.jkoolcloud.tnt4j.streams.parsers;

import static com.jkoolcloud.tnt4j.streams.fields.StreamFieldType.*;
import static com.jkoolcloud.tnt4j.streams.utils.SyslogStreamConstants.*;

import java.io.IOException;
import java.lang.Exception;
import java.nio.CharBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.SyslogStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.SyslogUtils;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

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
 * <li>EventType</li>
 * <li>EventName</li>
 * <li>Exception</li>
 * <li>UserName</li>
 * <li>ResourceName</li>
 * <li>Location</li>
 * <li>Tag</li>
 * <li>Correlator</li>
 * <li>ProcessId</li>
 * <li>ThreadId</li>
 * <li>Message</li>
 * <li>Severity</li>
 * <li>ApplName</li>
 * <li>ServerName</li>
 * <li>EndTime - resolved log event timestamp value in microseconds</li>
 * <li>ElapsedTime - calculated time difference between same host and app events in microseconds</li>
 * <li>MsgCharSet</li>
 * </ul>
 * <li>for activity properties:</li>
 * <ul>
 * <li>facility</li>
 * <li>level</li>
 * <li>hostname</li>
 * <li>hostaddr</li>
 * <li>version</li>
 * <li>priority</li>
 * </ul>
 * <li>maps of resolved additional custom activity properties:</li>
 * <ul>
 * <li>SyslogMap - map of resolved RFC 5424 structured data</li>
 * <li>SyslogVars - map of resolved application message contained (varName=varValue) variables</li>
 * </ul>
 * </ul>
 * <p>
 * This parser supports the following properties (in addition to those supported by {@link AbstractActivityMapParser}):
 * <ul>
 * <li>CharSet - name of char set used by Syslog lines parser. Default value - 'UTF-8'. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivitySyslogLineParser extends AbstractActivityMapParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivitySyslogLineParser.class);

	private static final String DEFAULT_CHAR_SET = Utils.UTF8;

	private String streamCharSet = DEFAULT_CHAR_SET;

	private SyslogParser syslogParser;

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
	 * {@inheritDoc}
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.lang.String}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return String.class.isInstance(data);
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();

			if (SyslogStreamConstants.PROP_CHAR_SET.equalsIgnoreCase(name)) {
				streamCharSet = value;

				logger().log(OpLevel.DEBUG,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
						name, value);
			}
		}
	}

	@Override
	protected Map<String, ?> getDataMap(Object data) {
		if (data == null) {
			return null;
		}

		String msg = (String) data;

		Map<String, Object> dataMap = new HashMap<String, Object>();

		try {
			dataMap = syslogParser.parseSyslogMessage(msg);
		} catch (Exception exc) {
			logger().log(OpLevel.ERROR, StreamsResources.getString(SyslogStreamConstants.RESOURCE_BUNDLE_NAME,
					"ActivitySyslogLineParser.line.parse.failed"), exc);
		}

		return dataMap;
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
	private class SyslogParser {
		private static final String SYSLOG_STRUCT_ID = "struct.id"; // NON-NLS

		// As defined in RFC 5424.
		private static final int MAX_SUPPORTED_VERSION = 1;

		private final Map<String, Long> EVENT_TIMESTAMP_MAP = new HashMap<String, Long>(8);

		/**
		 * Construct a new Syslog log lines parser.
		 */
		public SyslogParser() {
		}

		private CharBuffer stringToBuffer(String msg) {
			CharBuffer cb = CharBuffer.wrap(msg);
			cb.rewind();

			return cb;
		}

		/**
		 * Parse Syslog log line string making map of parsed fields.
		 *
		 * @param logLine
		 *            Syslog log line
		 * @return a map, or null if line is empty.
		 *
		 * @throws IOException
		 *             if the underlying stream fails, or unexpected chars occurs.
		 */
		public Map<String, Object> parseSyslogMessage(String logLine) throws IOException {
			CharBuffer cb = stringToBuffer(logLine);

			Integer priority = null;
			int c = read(cb);

			if (c == -1) {
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
				expect(cb, SPACE);
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
			Map<String, Object> map = new HashMap<String, Object>();
			int facility = priority == null ? DEFAULT_FACILITY : priority / 8;
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

			SyslogUtils.extractVariables(appMsg, map);
			String eventKey = String.format("%s/%s", (String) map.get(Location.name()), // NON-NLS
					(String) map.get(ResourceName.name()));
			long eventTime = date.getTimeInMillis();
			map.put(EndTime.name(), eventTime * 1000);
			map.put(ElapsedTime.name(), getUsecSinceLastEvent(eventKey, eventTime));

			return map;
		}

		/**
		 * Obtain elapsed microseconds since last event.
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
			int c = read(cb);

			switch (c) {
			case 'A':
				switch (read(cb)) {
				case 'p':
					skipWord(cb);
					return Calendar.APRIL;
				case 'u':
					skipWord(cb);
					return Calendar.AUGUST;
				default:
					return -1;
				}
			case 'D':
				skipWord(cb);
				return Calendar.DECEMBER;
			case 'F':
				skipWord(cb);
				return Calendar.FEBRUARY;
			case 'J':
				read(cb); // Second letter is ambiguous.
				read(cb); // Third letter is also ambiguous.
				switch (read(cb)) {
				case 'e':
					skipWord(cb);
					return Calendar.JUNE;
				case 'u':
					skipWord(cb);
					return Calendar.JANUARY;
				case 'y':
					skipWord(cb);
					return Calendar.JULY;
				default:
					return -1;
				}
			case 'M':
				read(cb); // Second letter is ambiguous.
				switch (read(cb)) {
				case 'r':
					skipWord(cb);
					return Calendar.MARCH;
				case 'y':
					skipWord(cb);
					return Calendar.MAY;
				default:
					return -1;
				}
			case 'N':
				skipWord(cb);
				return Calendar.NOVEMBER;
			case 'O':
				skipWord(cb);
				return Calendar.OCTOBER;
			case 'S':
				skipWord(cb);
				return Calendar.SEPTEMBER;
			default:
				return -1;
			}
		}

		/**
		 * Read a char and assert the value.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 * @param c
		 *            expected character
		 *
		 * @throws IOException
		 *             if expected and found characters differs
		 *
		 * @see #read(CharBuffer)
		 */
		private void expect(CharBuffer cb, int c) throws IOException {
			int d = read(cb);

			if (d != c) {
				throw new IOException(StreamsResources.getStringFormatted(SyslogStreamConstants.RESOURCE_BUNDLE_NAME,
						"ActivitySyslogLineParser.unexpected.char", (char) c, (char) d));
			}
		}

		/**
		 * Read until a non-whitespace char is found.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 *
		 * @see #read(CharBuffer)
		 * @see #unread(CharBuffer)
		 */
		private void skipSpaces(CharBuffer cb) {
			int c;

			while ((c = read(cb)) == SPACE) {
				continue;
			}

			unread(cb);
		}

		/**
		 * Read the next char, but then unread it.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 *
		 * @see #read(CharBuffer)
		 * @see #unread(CharBuffer)
		 */
		private int peek(CharBuffer cb) {
			int c = read(cb);

			unread(cb);

			return c;
		}

		/**
		 * Read the next char from buffer.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 * @return next char, or {@code -1} if no more chars
		 *
		 * @see CharBuffer#mark()
		 * @see CharBuffer#get()
		 */
		private int read(CharBuffer cb) {
			cb.mark();
			try {
				return cb.get();
			} catch (RuntimeException exc) {
				return -1;
			}
		}

		/**
		 * Push back character position to previous.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 *
		 * @see CharBuffer#reset()
		 */
		private void unread(CharBuffer cb) {
			cb.reset();
		}

		/**
		 * Read a positive integer from buffer.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 *
		 * @see #read(CharBuffer)
		 * @see #unread(CharBuffer)
		 * @see Character#isDigit(char)
		 */
		private int readInt(CharBuffer cb) {
			int c;
			int ret = 0;

			while (Character.isDigit(c = read(cb))) {
				ret = ret * 10 + (c - ZERO);
			}

			if (c != -1) {
				unread(cb);
			}

			return ret;
		}

		/**
		 * Read fractional part of number - digits after a decimal point.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 * @return a value in the range [0, 1)
		 *
		 * @see #read(CharBuffer)
		 * @see #unread(CharBuffer)
		 * @see Character#isDigit(char)
		 */
		private double readFractions(CharBuffer cb) {
			int c;
			int ret = 0;
			int order = 1;

			while (Character.isDigit(c = read(cb))) {
				ret = ret * 10 + (c - ZERO);
				order *= 10;
			}

			if (c != -1) {
				unread(cb);
			}

			return (double) ret / order;
		}

		/**
		 * Read until a end of the word and discard read chars. Word is terminated by whitespace char (' ') or end of
		 * buffer.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 *
		 * @see #read(CharBuffer)
		 * @see #unread(CharBuffer)
		 */
		private void skipWord(CharBuffer cb) {
			int c;

			do {
				c = read(cb);
			} while (c != SPACE && c != -1);

			if (c != -1) {
				unread(cb);
			}
		}

		/**
		 * Read a word into the given {@link StringBuilder}. Word is terminated by whitespace char (' ') or end of
		 * buffer.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 * @param sb
		 *            string builder to fill
		 *
		 * @see #read(CharBuffer)
		 * @see #unread(CharBuffer)
		 * @see StringBuilder#append(char)
		 */
		private void readWord(CharBuffer cb, StringBuilder sb) {
			int c;

			while ((c = read(cb)) != SPACE && c != -1) {
				sb.append((char) c);
			}

			if (c != -1) {
				unread(cb);
			}
		}

		/**
		 * Read a word from buffer as a string. Word is terminated by whitespace char (' ') or end of buffer.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 * @param sizeHint
		 *            an guess on how large string will be, in chars
		 * @return a valid, but perhaps empty, word.
		 *
		 * @see #readWord(CharBuffer, StringBuilder)
		 */
		private String readWord(CharBuffer cb, int sizeHint) {
			StringBuilder sb = new StringBuilder(sizeHint);
			readWord(cb, sb);

			return sb.toString();
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
		 * Read a defined number of chars or until end of buffer.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 * @param count
		 *            number of chars to read
		 * @return string containing read characters
		 *
		 * @see #read(CharBuffer)
		 * @see #unread(CharBuffer)
		 * @see StringBuilder#append(char)
		 */
		private String readChars(CharBuffer cb, int count) {
			StringBuilder sb = new StringBuilder(count);
			int c;
			int i = 0;

			while ((c = read(cb)) != -1 && i < count) {
				sb.append((char) c);
				i++;
			}

			unread(cb);

			return sb.toString();
		}

		/**
		 * Read a line from buffer as a string. Line is terminated by new line char ('\n') or end of buffer.
		 *
		 * @param cb
		 *            char buffer containing log line to read
		 * @param sizeHint
		 *            an guess on how large the line will be, in chars.
		 * @return read line string
		 *
		 * @see #read(CharBuffer)
		 * @see StringBuilder#append(char)
		 */
		private String readLine(CharBuffer cb, int sizeHint) {
			StringBuilder sb = new StringBuilder(sizeHint);
			int c;

			while ((c = read(cb)) != NL && c != -1) {
				if (c != RC) {
					sb.append((char) c);
				}
			}

			return sb.toString();
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

			Map<String, Object> sdm = new HashMap<String, Object>();

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
