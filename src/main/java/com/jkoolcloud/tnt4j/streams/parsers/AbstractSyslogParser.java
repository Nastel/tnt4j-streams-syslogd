/*
 * Copyright 2014-2017 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jkoolcloud.tnt4j.streams.parsers;

import static com.jkoolcloud.tnt4j.streams.fields.StreamFieldType.*;

import java.lang.Exception;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.SyslogStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class for abstract syslog entries data parser that assumes each activity data item is an Syslog server event
 * {@link org.graylog2.syslog4j.server.SyslogServerEventIF} or Syslog log line {@link String}. Parser resolved Syslog
 * entry data fields are put into {@link Map} and afterwards mapped into activity fields and properties according to
 * defined parser configuration.
 * <p>
 * This parser supports the following properties (in addition to those supported by {@link AbstractActivityMapParser}):
 * <ul>
 * <li>SuppressMessagesLevel - Syslog messages suppression level:
 * <ul>
 * <li>{@code 0} - output all Syslog messages</li>
 * <li>{@code -1} - output only the first occurrence of Syslog message</li>
 * <li>any other positive number - suppresses all Syslog messages except those that are multiples of that number</li>
 * </ul>
 * Default value - '{@value #DEFAULT_SUPPRESSION_LEVEL}'. (Optional)</li>
 * <li>SuppressIgnoredFields - Syslog message ignored fields list used to compare if message contents are same. Default
 * value - ['EndTime', 'ElapsedTime', 'Tag']. (Optional)</li>
 * <li>SuppressCacheSize - maximal Syslog messages suppression cache entries count. Default value -
 * '{@value #DEFAULT_MAX_CACHE_SIZE}'. (Optional)</li>
 * <li>SuppressCacheExpireDurationMinutes - Syslog messages suppression cache entries expiration duration value in
 * minutes. Default value - '{@value #DEFAULT_CACHE_EXPIRE_DURATION}'. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractSyslogParser extends AbstractActivityMapParser {

	/**
	 * Constant for default messages suppression cache maximum entries count.
	 */
	static final long DEFAULT_MAX_CACHE_SIZE = 100;
	/**
	 * Constant for default messages suppression cache entries expiration duration in minutes.
	 */
	static final long DEFAULT_CACHE_EXPIRE_DURATION = 10;
	/**
	 * Constant for default messages suppression level.
	 */
	static final int DEFAULT_SUPPRESSION_LEVEL = 0;

	/**
	 * Constant for default array of log entry suppression ignored fields.
	 */
	final static String[] DEFAULT_IGNORED_FIELDS = { EndTime.name(), ElapsedTime.name(), Tag.name() };

	private int suppressionLevel = DEFAULT_SUPPRESSION_LEVEL;
	private long cacheSize = DEFAULT_MAX_CACHE_SIZE;
	private long cacheExpireDuration = DEFAULT_CACHE_EXPIRE_DURATION;
	private List<String> ignoredFields = Arrays.asList(DEFAULT_IGNORED_FIELDS);

	private static final MessageDigest MSG_DIGEST = Utils.getMD5Digester();
	private Cache<String, Integer> msc;

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();

			if (SyslogStreamConstants.PROP_SUPPRESS_LEVEL.equalsIgnoreCase(name)) {
				suppressionLevel = NumberUtils.toInt(value, DEFAULT_SUPPRESSION_LEVEL);
				logger().log(OpLevel.DEBUG,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
						name, value);
			} else if (SyslogStreamConstants.PROP_SUPPRESS_CACHE_SIZE.equalsIgnoreCase(name)) {
				cacheSize = NumberUtils.toLong(value, DEFAULT_MAX_CACHE_SIZE);
				logger().log(OpLevel.DEBUG,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
						name, value);
			} else if (SyslogStreamConstants.PROP_SUPPRESS_CACHE_EXPIRE.equalsIgnoreCase(name)) {
				cacheExpireDuration = NumberUtils.toLong(value, DEFAULT_CACHE_EXPIRE_DURATION);
				logger().log(OpLevel.DEBUG,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
						name, value);
			} else if (SyslogStreamConstants.PROP_SUPPRESS_IGNORED_FIELDS.equalsIgnoreCase(name)) {
				if (StringUtils.isNotEmpty(value)) {
					ignoredFields = Arrays.asList(Utils.splitValue(value));
					logger().log(OpLevel.DEBUG,
							StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
							name, value);
				}
			}
		}
	}

	/**
	 * Determines if log entry has to be suppressed depending on {@link #suppressionLevel} value. Calculates MD5 hash
	 * for not ignored log entry fields. Having MD5 hash checks log message occurrences count in messages suppression
	 * cache. NOTE: cache entry lifetime depends on {@link #cacheSize} and {@link #cacheExpireDuration} values.
	 * <p>
	 * Log entry gets suppressed if:
	 * <ul>
	 * <li>{@link #suppressionLevel) value is {@code -1} and log entry occurs more than 1 time</li>
	 * <li>{@link #suppressionLevel} value is positive integer and log entry occurs non multiple time of that
	 * number</li>
	 * </ul>
	 *
	 * @param dataMap
	 *            log entry resolved fields map
	 * @return {@code null} if log entry gets suppressed, or same parameters defined {@code dataMap} if log entry is not
	 *         suppressed
	 */
	protected Map<String, Object> suppress(Map<String, Object> dataMap) {
		if (suppressionLevel == 0) {
			return dataMap;
		}

		if (msc == null) {
			msc = buildCache(cacheSize, cacheExpireDuration);
		}

		String byteData = new String(getMD5(dataMap, ignoredFields));

		Integer invocations = msc.getIfPresent(byteData);
		if (invocations == null) {
			msc.put(byteData, 1);
		} else {
			invocations++;

			if (suppressionLevel == -1) {
				logger().log(OpLevel.DEBUG, StreamsResources.getString(SyslogStreamConstants.RESOURCE_BUNDLE_NAME,
						"AbstractSyslogParser.suppressing.event1"), invocations);
				return null;
			}

			if (suppressionLevel > 0) {
				int evtSeqNumber = invocations % suppressionLevel;
				if (evtSeqNumber != 0) {
					logger().log(OpLevel.DEBUG, StreamsResources.getString(SyslogStreamConstants.RESOURCE_BUNDLE_NAME,
							"AbstractSyslogParser.suppressing.event2"), evtSeqNumber, suppressionLevel);
					return null;
				}
			}
		}

		return dataMap;
	}

	private static Cache<String, Integer> buildCache(long cSize, long duration) {
		return CacheBuilder.newBuilder().maximumSize(cSize).expireAfterAccess(duration, TimeUnit.MINUTES).build();
	}

	private byte[] getMD5(Map<String, Object> logDataMap, Collection<String> ignoredFields) {
		synchronized (MSG_DIGEST) {
			MSG_DIGEST.reset();

			updateDigest(MSG_DIGEST, logDataMap, ignoredFields, "");

			return MSG_DIGEST.digest();
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
}
