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

package com.jkoolcloud.tnt4j.streams.parsers;

import static com.jkoolcloud.tnt4j.streams.fields.StreamFieldType.*;

import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.configure.SyslogParserProperties;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.SyslogStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class for abstract syslog entries data parser that assumes each activity data item is an Syslog server event
 * {@link org.graylog2.syslog4j.server.SyslogServerEventIF} or Syslog log line {@link String}. Parser resolved Syslog
 * entry data fields are put into {@link Map} afterwards mapped into activity fields and properties according to defined
 * parser configuration.
 * <p>
 * This parser supports the following properties (in addition to those supported by {@link AbstractActivityMapParser}):
 * <ul>
 * <li>SuppressMessagesLevel - Syslog messages suppression level:
 * <ul>
 * <li>{@code 0} - output all Syslog messages</li>
 * <li>{@code -1} - output only the first occurrence of Syslog message</li>
 * <li>any other positive number - suppresses all Syslog messages except those that are multiples of that number</li>
 * </ul>
 * Default value - {@value #DEFAULT_SUPPRESSION_LEVEL}. (Optional)</li>
 * <li>SuppressIgnoredFields - Syslog message ignored fields list used to compare if message contents are same. Default
 * value - ['EndTime', 'ElapsedTime', 'Tag']. (Optional)</li>
 * <li>SuppressCacheSize - maximal Syslog messages suppression cache entries count. Default value -
 * {@value #DEFAULT_MAX_CACHE_SIZE}. (Optional)</li>
 * <li>SuppressCacheExpireDurationMinutes - Syslog messages suppression cache entries expiration duration value in
 * minutes. Default value - {@value #DEFAULT_CACHE_EXPIRE_DURATION}. (Optional)</li>
 * <li>FlattenStructuredData - flag indicating to flatten structured data map if there is only one structure available.
 * Default value - {@code false}. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractSyslogParser extends AbstractActivityMapParser {

	/**
	 * Constant for default messages suppression cache maximum entries count.
	 */
	public static final long DEFAULT_MAX_CACHE_SIZE = 100;
	/**
	 * Constant for default messages suppression cache entries expiration duration in minutes.
	 */
	public static final long DEFAULT_CACHE_EXPIRE_DURATION = 10;
	/**
	 * Constant for default messages suppression level.
	 */
	public static final int DEFAULT_SUPPRESSION_LEVEL = 0;

	/**
	 * Constant for default array of log entry suppression ignored fields.
	 */
	final static String[] DEFAULT_IGNORED_FIELDS = { EndTime.name(), ElapsedTime.name(), Tag.name() };

	private int suppressionLevel = DEFAULT_SUPPRESSION_LEVEL;
	private long cacheSize = DEFAULT_MAX_CACHE_SIZE;
	private long cacheExpireDuration = DEFAULT_CACHE_EXPIRE_DURATION;
	private List<String> ignoredFields = Arrays.asList(DEFAULT_IGNORED_FIELDS);
	private boolean flattenStructuredData = false;

	private static final MessageDigest MSG_DIGEST = Utils.getMD5Digester();
	private final Map<String, Long> EVENT_TIMESTAMP_MAP = new HashMap<>(8);

	private Cache<String, AtomicInteger> msc;

	protected final ReentrantLock digestLock = new ReentrantLock();
	protected final ReentrantLock cacheLock = new ReentrantLock();

	/**
	 * Constructs a new AbstractSyslogParser.
	 */
	protected AbstractSyslogParser() {
		super();
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (SyslogParserProperties.PROP_SUPPRESS_LEVEL.equalsIgnoreCase(name)) {
			suppressionLevel = NumberUtils.toInt(value, DEFAULT_SUPPRESSION_LEVEL);
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.setting", name, value);
		} else if (SyslogParserProperties.PROP_SUPPRESS_CACHE_SIZE.equalsIgnoreCase(name)) {
			cacheSize = NumberUtils.toLong(value, DEFAULT_MAX_CACHE_SIZE);
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.setting", name, value);
		} else if (SyslogParserProperties.PROP_SUPPRESS_CACHE_EXPIRE.equalsIgnoreCase(name)) {
			cacheExpireDuration = NumberUtils.toLong(value, DEFAULT_CACHE_EXPIRE_DURATION);
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.setting", name, value);
		} else if (SyslogParserProperties.PROP_SUPPRESS_IGNORED_FIELDS.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				ignoredFields = Arrays.asList(Utils.splitValue(value));
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.setting", name, value);
			}
		} else if (SyslogParserProperties.PROP_FLATTEN_STRUCTURED_DATA.equalsIgnoreCase(name)) {
			flattenStructuredData = Utils.toBoolean(value);
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.setting", name, value);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (SyslogParserProperties.PROP_SUPPRESS_LEVEL.equalsIgnoreCase(name)) {
			return suppressionLevel;
		}
		if (SyslogParserProperties.PROP_SUPPRESS_CACHE_SIZE.equalsIgnoreCase(name)) {
			return cacheSize;
		}
		if (SyslogParserProperties.PROP_SUPPRESS_CACHE_EXPIRE.equalsIgnoreCase(name)) {
			return cacheExpireDuration;
		}
		if (SyslogParserProperties.PROP_SUPPRESS_IGNORED_FIELDS.equalsIgnoreCase(name)) {
			return ignoredFields;
		}
		if (SyslogParserProperties.PROP_FLATTEN_STRUCTURED_DATA.equalsIgnoreCase(name)) {
			return flattenStructuredData;
		}

		return super.getProperty(name);
	}

	/**
	 * Determines if log entry has to be suppressed depending on {@link #suppressionLevel} value. Calculates MD5 hash
	 * for not ignored log entry fields. Having MD5 hash checks log message occurrences count in messages suppression
	 * cache. NOTE: cache entry lifetime depends on {@link #cacheSize} and {@link #cacheExpireDuration} values.
	 * <p>
	 * Log entry gets suppressed if:
	 * <ul>
	 * <li>{@link #suppressionLevel} value is {@code -1} and log entry occurs more than 1 time</li>
	 * <li>{@link #suppressionLevel} value is positive integer and log entry occurs non multiple time of that
	 * number</li>
	 * </ul>
	 *
	 * @param dataMap
	 *            log entry resolved fields map
	 * @return {@code null} if log entry gets suppressed, or same parameters defined {@code dataMap} if log entry is not
	 *         suppressed
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Object> suppress(Map<String, Object> dataMap) {
		if (suppressionLevel != 0) {
			AtomicInteger invocations;
			cacheLock.lock();
			try {
				if (msc == null) {
					msc = buildCache(cacheSize, cacheExpireDuration);
				}

				String byteData = Utils.getString(getMD5(dataMap, ignoredFields));

				invocations = msc.getIfPresent(byteData);
				if (invocations == null) {
					invocations = new AtomicInteger();
					msc.put(byteData, invocations);
				}
			} finally {
				cacheLock.unlock();
			}

			if (invocations.incrementAndGet() > 1) {
				if (suppressionLevel == -1) {
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
							"AbstractSyslogParser.suppressing.event1", invocations);
					return null;
				}

				if (suppressionLevel > 0) {
					int evtSeqNumber = invocations.get() % suppressionLevel;
					if (evtSeqNumber != 0) {
						logger().log(OpLevel.DEBUG,
								StreamsResources.getBundle(SyslogStreamConstants.RESOURCE_BUNDLE_NAME),
								"AbstractSyslogParser.suppressing.event2", evtSeqNumber, suppressionLevel);
						return null;
					}
				}
			}
		}

		if (flattenStructuredData) {
			Object structData = dataMap.get(SyslogStreamConstants.FIELD_SYSLOG_MAP);
			if (structData instanceof Map) {
				Map<String, Map<String, Object>> structDataMap = (Map<String, Map<String, Object>>) structData;

				if (structDataMap.size() == 1) {
					Map.Entry<String, Map<String, Object>> sdme = structDataMap.entrySet().iterator().next();
					Map<String, Object> sdMap = sdme.getValue();
					sdMap.put(SyslogStreamConstants.SYSLOG_STRUCT_ID, sdme.getKey());
					dataMap.put(SyslogStreamConstants.FIELD_SYSLOG_MAP, sdMap);
				}
			}
		}

		return dataMap;
	}

	private static Cache<String, AtomicInteger> buildCache(long cSize, long duration) {
		return CacheBuilder.newBuilder().maximumSize(cSize).expireAfterAccess(duration, TimeUnit.MINUTES).build();
	}

	private byte[] getMD5(Map<String, Object> logDataMap, Collection<String> ignoredFields) {
		digestLock.lock();
		try {
			MSG_DIGEST.reset();

			updateDigest(MSG_DIGEST, logDataMap, ignoredFields, "");

			return MSG_DIGEST.digest();
		} finally {
			digestLock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	private void updateDigest(MessageDigest digest, Map<String, Object> logDataMap, Collection<String> ignoredFields,
			String keyPrefix) {
		for (Map.Entry<String, Object> ldme : logDataMap.entrySet()) {
			String fKey = keyPrefix + ldme.getKey();

			if (ignoredFields != null && ignoredFields.contains(fKey)) {
				continue;
			}

			if (ldme.getValue() instanceof Map) {
				updateDigest(digest, (Map<String, Object>) ldme.getValue(), ignoredFields, fKey + nodePathDelim);
			} else {
				digest.update(Utils.toString(ldme.getValue()).getBytes());
			}
		}
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
	protected long getUsecSinceLastEvent(String eventKey, long eventTime) {
		synchronized (EVENT_TIMESTAMP_MAP) {
			Long prev_ts = EVENT_TIMESTAMP_MAP.put(eventKey, eventTime);

			if (prev_ts == null) {
				prev_ts = eventTime;
			}

			return TimeUnit.MILLISECONDS.toMicros(eventTime - prev_ts);
		}
	}
}
