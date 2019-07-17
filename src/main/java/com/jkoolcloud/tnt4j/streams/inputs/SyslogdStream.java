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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.net.SocketAddress;

import org.apache.commons.lang3.StringUtils;
import org.graylog2.syslog4j.SyslogConstants;
import org.graylog2.syslog4j.server.*;
import org.graylog2.syslog4j.server.impl.net.tcp.TCPNetSyslogServerConfigIF;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.SyslogStreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.SyslogStreamConstants;

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
 * Actual if 'Protocol' property value is set to 'tcp'. Default value - '0'. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class SyslogdStream extends AbstractBufferedStream<SyslogServerEventIF> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(SyslogdStream.class);

	private static final String DEFAULT_HOST = "0.0.0.0"; // NON-NLS

	// Stream properties
	private String protocol = SyslogConstants.TCP;
	private String host = DEFAULT_HOST;
	private int port = SyslogConstants.SYSLOG_PORT_DEFAULT;
	private int timeout = 0;

	private SyslogDataReceiver syslogDataReceiver;

	/**
	 * Constructs an empty SyslogdStream. Requires configuration settings to set input stream source.
	 */
	public SyslogdStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_HOST.equalsIgnoreCase(name)) {
			return host;
		}
		if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			return port;
		}
		if (SyslogStreamProperties.PROP_PROTOCOL.equalsIgnoreCase(name)) {
			return protocol;
		}
		if (SyslogStreamProperties.PROP_TIMEOUT.equalsIgnoreCase(name)) {
			return timeout;
		}

		return super.getProperty(name);
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (StreamProperties.PROP_HOST.equalsIgnoreCase(name)) {
			host = value;
		} else if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			port = Integer.parseInt(value);
		} else if (SyslogStreamProperties.PROP_PROTOCOL.equalsIgnoreCase(name)) {
			protocol = value;
		} else if (SyslogStreamProperties.PROP_TIMEOUT.equalsIgnoreCase(name)) {
			timeout = Integer.parseInt(value);
		}
	}

	@Override
	protected void applyProperties() throws Exception {
		super.applyProperties();

		if (StringUtils.isEmpty(protocol)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined", SyslogStreamProperties.PROP_PROTOCOL));
		}

		if (!SyslogServer.exists(protocol)) {
			throw new IllegalArgumentException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"TNTInputStream.property.illegal", SyslogStreamProperties.PROP_PROTOCOL, protocol));
		}
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		syslogDataReceiver = new SyslogDataReceiver();
		syslogDataReceiver.initialize();
	}

	@Override
	protected void start() throws Exception {
		super.start();

		syslogDataReceiver.start();

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"TNTInputStream.stream.start", getClass().getSimpleName(), getName());
	}

	@Override
	protected void cleanup() {
		if (syslogDataReceiver != null) {
			syslogDataReceiver.shutdown();
		}

		super.cleanup();
	}

	@Override
	protected boolean isInputEnded() {
		return syslogDataReceiver.isInputEnded();
	}

	@Override
	protected long getActivityItemByteSize(SyslogServerEventIF item) {
		byte[] payload = item.getRaw();

		return payload == null ? 0 : payload.length;
	}

	private class SyslogDataReceiver extends InputProcessor implements SyslogServerSessionEventHandlerIF {

		private SyslogServerIF server;

		private SyslogDataReceiver() {
			super("SyslogdStream.SyslogDataReceiver"); // NON-NLS
		}

		/**
		 * Input data receiver initialization - Syslog server configuration.
		 *
		 * @param params
		 *            initialization parameters array
		 *
		 * @throws Exception
		 *             if fails to initialize data receiver and configure Syslog server
		 */
		@Override
		protected void initialize(Object... params) throws Exception {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
					"SyslogdStream.starting.server", SyslogServer.getVersion());

			SyslogServerIF syslogServer = SyslogServer.getInstance(protocol);

			SyslogServerConfigIF syslogServerConfig = syslogServer.getConfig();
			syslogServerConfig.setUseStructuredData(true);

			if (host != null) {
				syslogServerConfig.setHost(host);
			}
			syslogServerConfig.setPort(port);

			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
					"SyslogdStream.server.params", protocol, host, port);

			if (timeout > 0) {
				if (syslogServerConfig instanceof TCPNetSyslogServerConfigIF) {
					((TCPNetSyslogServerConfigIF) syslogServerConfig).setTimeout(timeout);
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
							"SyslogdStream.server.timeout", timeout);
				} else {
					logger().log(OpLevel.WARNING,
							StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
							"SyslogdStream.server.timeout.unsupported", protocol);
				}
			}

			syslogServerConfig.addEventHandler(this);

			// if (!quiet) {
			// SyslogServerSessionEventHandlerIF eventHandler = new JsonSyslogServerEventHandler(System.out);
			// syslogServerConfig.addEventHandler(eventHandler);
			// }
			//
			// if (printSyslog) {
			// SyslogServerSessionEventHandlerIF eventHandler = SystemOutSyslogServerEventHandler.create();
			// syslogServerConfig.addEventHandler(eventHandler);
			// }

			server = SyslogServer.getInstance(protocol);
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
					"SyslogdStream.server.initialized", SyslogServer.getVersion());
		}

		/**
		 * Starts Syslog server to receive incoming data. Shuts down this data receiver if exception occurs.
		 */
		@Override
		public void run() {
			if (server != null) {
				server.setThread(this);
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
						"SyslogdStream.server.ready", SyslogServer.getVersion());
				server.run();
			}
		}

		/**
		 * Closes Syslog server.
		 *
		 * @throws Exception
		 *             if fails to close opened resources due to internal error
		 */
		@Override
		void closeInternals() throws Exception {
			if (server != null) {
				server.shutdown();
			}
		}

		@Override
		public Object sessionOpened(SyslogServerIF server, SocketAddress address) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
					"SyslogdStream.session.opened", server, address);
			return null;
		}

		@Override
		public void event(Object session, SyslogServerIF server, SocketAddress address,
				SyslogServerEventIF syslogEvent) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
					"SyslogdStream.event.received", syslogEvent.getMessage());
			addInputToBuffer(syslogEvent);
		}

		@Override
		public void exception(Object session, SyslogServerIF server, SocketAddress address, Exception e) {
			logger().log(OpLevel.ERROR, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
					"SyslogdStream.session.exception", session, server, address, e); // NOTE: exception logging
		}

		@Override
		public void sessionClosed(Object session, SyslogServerIF server, SocketAddress address, boolean timeout) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
					"SyslogdStream.session.closed", session, server, address, timeout);
		}

		@Override
		public void initialize(SyslogServerIF server) {
			// NOTHING TO DO
		}

		@Override
		public void destroy(SyslogServerIF server) {
			// NOTHING TO DO
			// shutdown();
		}
	}
}
