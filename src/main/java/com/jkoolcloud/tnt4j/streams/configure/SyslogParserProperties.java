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

package com.jkoolcloud.tnt4j.streams.configure;

/**
 * Lists predefined property names used by TNT4-Streams Syslog RAW data parsers.
 *
 * @version $Revision: 1 $
 */
public interface SyslogParserProperties extends ParserProperties {

	/**
	 * Constant for name of built-in parser {@value} property.
	 */
	String PROP_CHARSET = "CharSet"; // NON-NLS

	/**
	 * Constant for name of built-in parser {@value} property.
	 */
	String PROP_SUPPRESS_LEVEL = "SuppressMessagesLevel"; // NON-NLS

	/**
	 * Constant for name of built-in parser {@value} property.
	 */
	String PROP_SUPPRESS_CACHE_SIZE = "SuppressCacheSize"; // NON-NLS

	/**
	 * Constant for name of built-in parser {@value} property.
	 */
	String PROP_SUPPRESS_CACHE_EXPIRE = "SuppressCacheExpireDurationMinutes"; // NON-NLS

	/**
	 * Constant for name of built-in parser {@value} property.
	 */
	String PROP_SUPPRESS_IGNORED_FIELDS = "SuppressIgnoredFields"; // NON-NLS

	/**
	 * Constant for name of built-in parser {@value} property.
	 */
	String PROP_FLATTEN_STRUCTURED_DATA = "FlattenStructuredData"; // NON-NLS
}
