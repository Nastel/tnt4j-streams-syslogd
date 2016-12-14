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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.SyslogStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.SyslogUtils;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * TODO
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

	private class SyslogParser {
		private static final String SYSLOG_STRUCT_ID = "struct.id"; // NON-NLS

		// As defined in RFC 5424.
		private static final int MAX_SUPPORTED_VERSION = 1;

		/**
		 * Construct a new Syslog protocol parser. Tags are parsed, and the encoding is assumed to be UTF-8.
		 */
		public SyslogParser() {
		}

		private ByteBuffer stringToBytes(String msg) {
			byte[] bytes = msg.getBytes();
			ByteBuffer bb = ByteBuffer.allocate(bytes.length);
			bb.put(bytes);
			bb.rewind();

			return bb;
		}

		/**
		 * Read the next Syslog message from the stream.
		 *
		 * @return a map, or null if message is empty.
		 *
		 * @throws IOException
		 *             if the underlying stream fails, or unexpected bytes are seen.
		 */
		public Map<String, Object> parseSyslogMessage(String msg) throws IOException {
			ByteBuffer bb = stringToBytes(msg);

			Integer priority = null;
			int c = read(bb);

			if (c == -1) {
				return null;
			}

			if (c == '<') {
				priority = readInt(bb);

				expect(bb, '>');
			} else {
				unread(bb);
			}

			int version = 0;
			Calendar cal;

			if (Character.isDigit(peek(bb))) {
				// Assume ISO date and time
				int y = readInt(bb);

				c = read(bb);

				if (c == SPACE) {
					// Assume this is a RFC 5424 message.
					version = y;

					if (version > MAX_SUPPORTED_VERSION) {
						throw new IOException(
								StreamsResources.getStringFormatted(SyslogStreamConstants.RESOURCE_BUNDLE_NAME,
										"ActivitySyslogLineParser.unsupported.version", version));
					}

					skipSpaces(bb);
					y = readInt(bb);
					expect(bb, MINUS);
				} else if (c != MINUS) {
					throw new IOException(
							StreamsResources.getStringFormatted(SyslogStreamConstants.RESOURCE_BUNDLE_NAME,
									"ActivitySyslogLineParser.unexpected.char", MINUS, (char) c));
				}

				int m = readInt(bb);
				expect(bb, MINUS);
				int d = readInt(bb);

				c = read(bb);

				if (c != 'T' && c != SPACE) {
					throw new IOException(
							StreamsResources.getStringFormatted(SyslogStreamConstants.RESOURCE_BUNDLE_NAME,
									"ActivitySyslogLineParser.unexpected.char", 'T', (char) c));
				}

				int hh = readInt(bb);
				expect(bb, COLON);
				int mm = readInt(bb);
				expect(bb, COLON);
				int ss = readInt(bb);
				double sf = 0;

				c = read(bb);

				if (c == '.') {
					// Fractions of seconds
					sf = readFractions(bb);
					c = read(bb);
				}

				int tz = 0;

				if (c == 'Z') {
					// UTC
				} else if (c == MINUS) {
					tz = readInt(bb);

					if (peek(bb) == COLON) {
						read(bb);
						tz = -(tz * 60 + readInt(bb));
					}
				} else if (c == '+') {
					tz = readInt(bb);

					if (peek(bb) == COLON) {
						read(bb);
						tz = tz * 60 + readInt(bb);
					}
				}

				cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.getDefault());

				cal.set(y, m - 1, d, hh, mm, ss);
				cal.set(Calendar.MILLISECOND, (int) (sf * 1000));
				cal.add(Calendar.MINUTE, tz);
			} else {
				// Assume BSD date and time
				int m = readMonthAbbreviation(bb);

				expect(bb, SPACE);
				skipSpaces(bb);

				int d = readInt(bb);

				expect(bb, SPACE);
				skipSpaces(bb);

				int hh = readInt(bb);

				expect(bb, COLON);

				int mm = readInt(bb);

				expect(bb, COLON);

				int ss = readInt(bb);

				cal = new GregorianCalendar(Locale.ROOT);

				cal.set(Calendar.MONTH, m);
				cal.set(Calendar.DAY_OF_MONTH, d);
				cal.set(Calendar.HOUR_OF_DAY, hh);
				cal.set(Calendar.MINUTE, mm);
				cal.set(Calendar.SECOND, ss);
				cal.set(Calendar.MILLISECOND, 0);
			}

			expect(bb, SPACE);
			skipSpaces(bb);

			String hostName = readWord(bb, 32);

			expect(bb, SPACE);

			String appName = null;
			String procId = null;
			String msgId = null;
			Map<String, Object> structuredData = null;

			if (version >= 1) {
				appName = readWordOrNil(bb, 20);
				expect(bb, SPACE);
				procId = readWordOrNil(bb, 20);
				expect(bb, SPACE);
				msgId = readWordOrNil(bb, 20);
				expect(bb, SPACE);
				structuredData = readStructuredData(bb);
				expect(bb, SPACE);
			} else if (version == 0) {
				// Try to find a colon terminated tag.
				appName = readTag(bb);
				if (peek(bb) == OB) {
					procId = readPid(bb);
				}
				expect(bb, COLON);
			}

			appName = StringUtils.isEmpty(appName) ? UNKNOWN : appName;

			skipSpaces(bb);

			String appMsg = readLine(bb, 128);

			return createFieldMap(version, priority, cal, hostName, appName, procId, msgId, structuredData, appMsg);
		}

		/**
		 * Skip an entire line. The line is terminated by new line. End of line is silently ignored. Useful if a parsing
		 * failure has occurred and you want to skip the message.
		 */
		public void skipLine(ByteBuffer bb) {
			int c;

			do {
				c = read(bb);
			} while (c != NL && c != -1);
		}

		/**
		 * Create a map from the given parameters.
		 *
		 * @param version
		 *            the syslog version, 0 for RFC 3164
		 * @param priority
		 *            the syslog priority, according to RFC 5424
		 * @param date
		 *            the timestamp of the message. Note that timezone matters
		 * @param hostName
		 *            the host name
		 * @param appName
		 *            the application name
		 * @param procId
		 *            the process id
		 * @param msgId
		 *            the RFC 5424 msg-id
		 * @param structuredData
		 *            the RFC 5424 structured-data
		 * @param appMsg
		 *            the application message body
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
			String locationKey = (String) map.get(Location.name()) + '/' + (String) map.get(ResourceName.name());
			map.put(EndTime.name(), date.getTimeInMillis() * 1000);
			map.put(ElapsedTime.name(), SyslogUtils.getUsecSinceLastEvent(locationKey)); // TODO: calculate from real
																							// timestamp differences
			return map;
		}

		/**
		 * Read a month value as an English abbreviation. See RFC 3164, Sec. 4.1.2.
		 *
		 * @param bb
		 *            byte buffer containing log line to read
		 * @return resolved month index, or {@code -1} if unknown
		 */
		private int readMonthAbbreviation(ByteBuffer bb) {
			int c = read(bb);

			switch (c) {
			case 'A':
				switch (read(bb)) {
				case 'p':
					skipWord(bb);
					return Calendar.APRIL;
				case 'u':
					skipWord(bb);
					return Calendar.AUGUST;
				default:
					return -1;
				}
			case 'D':
				skipWord(bb);
				return Calendar.DECEMBER;
			case 'F':
				skipWord(bb);
				return Calendar.FEBRUARY;
			case 'J':
				read(bb); // Second letter is ambiguous.
				read(bb); // Third letter is also ambiguous.
				switch (read(bb)) {
				case 'e':
					skipWord(bb);
					return Calendar.JUNE;
				case 'u':
					skipWord(bb);
					return Calendar.JANUARY;
				case 'y':
					skipWord(bb);
					return Calendar.JULY;
				default:
					return -1;
				}
			case 'M':
				read(bb); // Second letter is ambiguous.
				switch (read(bb)) {
				case 'r':
					skipWord(bb);
					return Calendar.MARCH;
				case 'y':
					skipWord(bb);
					return Calendar.MAY;
				default:
					return -1;
				}
			case 'N':
				skipWord(bb);
				return Calendar.NOVEMBER;
			case 'O':
				skipWord(bb);
				return Calendar.OCTOBER;
			case 'S':
				skipWord(bb);
				return Calendar.SEPTEMBER;
			default:
				return -1;
			}
		}

		/**
		 * Read a byte and assert the value.
		 *
		 * @throws IOException
		 *             if the character was unexpected
		 */
		private void expect(ByteBuffer bb, int c) throws IOException {
			int d = read(bb);

			if (d != c) {
				throw new IOException(StreamsResources.getStringFormatted(SyslogStreamConstants.RESOURCE_BUNDLE_NAME,
						"ActivitySyslogLineParser.unexpected.char", (char) c, (char) d));
			}
		}

		/**
		 * Read until a non-whitespace ASCII byte is seen.
		 */
		private void skipSpaces(ByteBuffer bb) {
			int c;

			while ((c = read(bb)) == SPACE) {
				continue;
			}

			unread(bb);
		}

		/**
		 * Read the next byte, but then unread it again.
		 */
		private int peek(ByteBuffer bb) {
			int c = read(bb);

			unread(bb);

			return c;
		}

		/**
		 * Read the next byte.
		 *
		 * @return the byte.
		 */
		private int read(ByteBuffer bb) {
			bb.mark();
			try {
				return (char) bb.get();
			} catch (BufferUnderflowException exc) {
				return -1;
			}
		}

		/**
		 * Push back a character. Only a single character can be pushed back simultaneously.
		 */
		private void unread(ByteBuffer bb) {
			bb.reset();
		}

		/**
		 * Read a positive integer and convert it from decimal text form. End of line silently terminates the number.
		 */
		private int readInt(ByteBuffer bb) {
			int c;
			int ret = 0;

			while (Character.isDigit(c = read(bb))) {
				ret = ret * 10 + (c - ZERO);
			}

			if (c != -1) {
				unread(bb);
			}

			return ret;
		}

		/**
		 * Read fractions (digits after a decimal point.)
		 *
		 * @return a value in the range [0, 1).
		 */
		private double readFractions(ByteBuffer bb) {
			int c;
			int ret = 0;
			int order = 1;

			while (Character.isDigit(c = read(bb))) {
				ret = ret * 10 + (c - ZERO);
				order *= 10;
			}

			if (c != -1) {
				unread(bb);
			}

			return (double) ret / order;
		}

		/**
		 * Read until a space or end of line. The input is discarded.
		 */
		private void skipWord(ByteBuffer bb) {
			int c;

			do {
				c = read(bb);
			} while (c != SPACE && c != -1);

			if (c != -1) {
				unread(bb);
			}
		}

		/**
		 * Read a word into the given output stream. Usually the output stream will be a StringBuilder.
		 */
		private void readWord(ByteBuffer bb, StringBuilder sb) {
			int c;

			while ((c = read(bb)) != SPACE && c != -1) {
				sb.append((char) c);
			}

			if (c != -1) {
				unread(bb);
			}
		}

		/**
		 * Read a word (until next ASCII space or end of line) as a byte array.
		 *
		 * @param sizeHint
		 *            an guess on how large string will be, in bytes.
		 *
		 * @return a valid, but perhaps empty, word.
		 */
		private String readWord(ByteBuffer bb, int sizeHint) {
			StringBuilder sb = new StringBuilder(sizeHint);
			readWord(bb, sb);

			return sb.toString();
		}

		/**
		 * Read a word (until next space or end of line) to a string. If the complete word is "-", returns {@code null}.
		 *
		 * @param sizeHint
		 *            an guess on how large string will be, in bytes
		 * @param bb
		 *            byte buffer containing log line to read
		 * @return word string read from log line
		 */
		private String readWordOrNil(ByteBuffer bb, int sizeHint) {
			String ret = readWord(bb, sizeHint);

			if (ret.length() == 1 && ret.charAt(0) == MINUS) {
				return null;
			}

			return ret;
		}

		/**
		 * Read a defined number of chars (or until EOL).
		 *
		 * @param bb
		 *            byte buffer containing log line to read
		 * @param count
		 *            number of chars to read
		 * @return string containing read characters
		 */
		private String readChars(ByteBuffer bb, int count) {
			StringBuilder sb = new StringBuilder(count);
			int c;
			int i = 0;

			while ((c = read(bb)) != -1 && i < count) {
				sb.append((char) c);
				i++;
			}

			unread(bb);

			return sb.toString();
		}

		/**
		 * Read a line until next '\n' or EOL to a string.
		 *
		 * @param bb
		 *            byte buffer containing log line to read
		 * @param sizeHint
		 *            an guess on how large the line will be, in bytes.
		 * @return read line string
		 */
		private String readLine(ByteBuffer bb, int sizeHint) {
			StringBuilder sb = new StringBuilder(sizeHint);
			int c;

			while ((c = read(bb)) != NL && c != -1) {
				if (c != RC) {
					sb.append((char) c);
				}
			}

			return sb.toString();
		}

		/**
		 * Read a RFC 3164 tag. Tags end with one of: ':[\r\n'.
		 *
		 * @param bb
		 *            byte buffer containing log line to read
		 * @return resolved tag string
		 *
		 */
		private String readTag(ByteBuffer bb) {
			StringBuilder sb = new StringBuilder(16);
			int c;

			while ((c = read(bb)) != COLON && c != OB && c != RC && c != NL) {
				sb.append((char) c);
			}

			unread(bb);

			return sb.toString();
		}

		/**
		 * Read a RFC 3164 pid. The format is "[1234]".
		 *
		 * @param bb
		 *            byte buffer containing log line to read
		 * @return resolved process id string
		 *
		 * @throws IOException
		 *             if unexpected character is found while parsing
		 */
		private String readPid(ByteBuffer bb) throws IOException {
			StringBuilder sb = new StringBuilder(8);
			int c;

			expect(bb, OB);

			while ((c = read(bb)) != CB && c != RC && c != NL) {
				sb.append((char) c);
			}

			return sb.toString();
		}

		/**
		 * Read RFC 5424 structured data map.
		 *
		 * @param bb
		 *            byte buffer containing log line to read
		 * @return resolved structured data map
		 *
		 * @throws IOException
		 *             if unexpected character is found while parsing
		 */
		private Map<String, Object> readStructuredData(ByteBuffer bb) throws IOException {
			int c = read(bb);

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
				while ((c = read(bb)) != SPACE && c != CB) {
					sb.append((char) c);
				}
				sdm.put(SYSLOG_STRUCT_ID, sb.toString());
				sb.setLength(0);

				String paramName;
				while (c == SPACE) {
					// Read parameter name
					while ((c = read(bb)) != EQ) {
						sb.append((char) c);
					}
					paramName = sb.toString();
					sb.setLength(0);

					expect(bb, QUOTE);

					// Read parameter data
					while ((c = read(bb)) != QUOTE) {
						sb.append((char) c);

						if (c == SyslogStreamConstants.SLASH) {
							c = read(bb);
							sb.append((char) c);
						}
					}
					sdm.put(paramName, sb.toString());
					sb.setLength(0);

					c = read(bb);
				}

				if (c != CB) {
					throw new IOException(
							StreamsResources.getStringFormatted(SyslogStreamConstants.RESOURCE_BUNDLE_NAME,
									"ActivitySyslogLineParser.unexpected.char", CB, (char) c));
				}

				c = read(bb);
			}

			unread(bb);

			return sdm;
		}
	}
}
