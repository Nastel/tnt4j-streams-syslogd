/*
 * Copyright (C) 2015-2023, JKOOL LLC.
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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.graylog2.syslog4j.Syslog;
import org.graylog2.syslog4j.SyslogConfigIF;
import org.graylog2.syslog4j.SyslogIF;
import org.graylog2.syslog4j.impl.message.processor.structured.StructuredSyslogMessageProcessor;
import org.graylog2.syslog4j.server.SyslogServerEventIF;
import org.graylog2.syslog4j.server.SyslogServerIF;
import org.graylog2.syslog4j.util.SyslogUtility;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.SyslogStreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.SyslogStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements Syslog server {@link SyslogServerIF} based activities stream, where each Syslog event data is assumed to
 * represent a single activity or event which should be recorded.
 * <p>
 * This activity stream requires parsers that can support JMS {@link SyslogServerEventIF} data.
 * <p>
 * This activity stream supports the following properties (in addition to those supported by
 * {@link AbstractBufferedStream}):
 * <ul>
 * <li>Host - host name to run Syslog server. (Required)</li>
 * <li>Port - port number to run Syslog server. Default value - '514'. (Optional)</li>
 * <li>Protocol - protocol of Syslog server communication: one of 'tcp' or 'udp' . Default value - 'tcp'.
 * (Optional)</li>
 * <li>Timeout - server communication timeout, where '0' means - server implementation dependent timeout handling.
 * Default value - '0'. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class SyslogdFileStream extends SyslogdStream {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(SyslogdFileStream.class);

	// Stream properties
	private String fileName = null;
	private String facility = "USER"; // NON-NLS
	private String level = "INFO"; // NON-NLS

	/**
	 * Constructs an empty SyslogdFileStream. Requires configuration settings to set input stream source.
	 */
	public SyslogdFileStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			return fileName;
		}
		if (SyslogStreamProperties.PROP_FACILITY.equalsIgnoreCase(name)) {
			return facility;
		}
		if (SyslogStreamProperties.PROP_LEVEL.equalsIgnoreCase(name)) {
			return level;
		}

		return super.getProperty(name);
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			fileName = value;
		} else if (SyslogStreamProperties.PROP_FACILITY.equalsIgnoreCase(name)) {
			facility = value;
		} else if (SyslogStreamProperties.PROP_LEVEL.equalsIgnoreCase(name)) {
			level = value;
		}
	}

	@Override
	protected void applyProperties() throws Exception {
		super.applyProperties();

		String protocol = (String) getProperty(SyslogStreamProperties.PROP_PROTOCOL);
		if (StringUtils.isNotEmpty(fileName) && !Syslog.exists(protocol)) {
			throw new IllegalArgumentException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"TNTInputStream.property.illegal", SyslogStreamProperties.PROP_PROTOCOL, protocol));
		}
	}

	@Override
	protected void start() throws Exception {
		super.start();

		if (StringUtils.isNotEmpty(fileName)) {
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					sendSyslogFromFile();
					halt(false);
				}
			});

			t.start();
		}
	}

	@Override
	protected void cleanup() {
		Syslog.shutdown();
		super.cleanup();
	}

	private void sendSyslogFromFile() {
		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
				"SyslogdStream.starting.syslog", Syslog.getVersion());

		String protocol = (String) getProperty(SyslogStreamProperties.PROP_PROTOCOL);
		String host = (String) getProperty(StreamProperties.PROP_HOST);
		int port = (Integer) getProperty(StreamProperties.PROP_PORT);

		SyslogIF syslog = Syslog.getInstance(protocol);
		SyslogConfigIF syslogConfig = syslog.getConfig();
		// syslogConfig.setUseStructuredData(true);

		if (host != null) {
			syslogConfig.setHost(host);
		}

		syslogConfig.setPort(port);

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
				"SyslogdStream.syslog.params", protocol, host, port);

		syslogConfig.setFacility(facility);

		try {
			sendFromTextFile(syslog);
		} catch (Exception exc) {
			Utils.logThrowable(logger(), OpLevel.WARNING,
					StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
					"SyslogdStream.file.send.failed", exc);
		} finally {
			Syslog.shutdown();
		}
	}

	private void sendFromTextFile(SyslogIF syslog) throws IOException, InterruptedException {
		int syslogLevel = SyslogUtility.getLevel(level);
		InputStream is = fileName == null ? System.in : Files.newInputStream(Paths.get(fileName));

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
				"SyslogdStream.file.send.start", fileName);

		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = br.readLine()) != null && !line.isEmpty()) {
				if (!line.startsWith("{")) {
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
							"SyslogdStream.file.send.line", facility, level, line);
					syslog.log(syslogLevel, line);
				} else {
					jsonSyslog(syslog, line);
				}
			}
		}
	}

	private void jsonSyslog(SyslogIF syslog, String line) throws InterruptedException {
		DocumentContext dc = JsonPath.parse(line);
		// JsonElement jelement = jparser.parse(line);
		// JsonObject jobject = jelement.getAsJsonObject();
		long offset_usec = dc.read("$['offset.usec']", Long.class); // NON-NLS
		String facility = dc.read("$.facility", String.class); // NON-NLS
		String level = dc.read("$.level", String.class); // NON-NLS
		String msg = dc.read("$.msg", String.class); // NON-NLS
		String appl = dc.read("$.appl", String.class); // NON-NLS
		String pid = dc.read("$.pid", String.class); // NON-NLS

		if (appl != null) {
			StructuredSyslogMessageProcessor mpr = new StructuredSyslogMessageProcessor(appl);
			mpr.setProcessId(pid);
			syslog.setStructuredMessageProcessor(mpr);
			syslog.getConfig().setUseStructuredData(true);
		} else {
			syslog.getConfig().setUseStructuredData(false);
		}
		if (!syslog.getConfig().isUseStructuredData()) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
					"SyslogdStream.file.send.json.line", offset_usec, syslog.getConfig().isUseStructuredData(),
					facility, level, msg);
		} else {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
					"SyslogdStream.file.send.json.line2", offset_usec, syslog.getConfig().isUseStructuredData(),
					facility, level, appl, pid, msg);
		}

		// Thread.sleep(offset_usec / 1000);

		syslog.getConfig().setFacility(facility);
		syslog.log(SyslogUtility.getLevel(level), msg);
	}
}
