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

package com.jkoolcloud.tnt4j.streams.utils;

import org.graylog2.syslog4j.util.SyslogUtility;

import com.jkoolcloud.tnt4j.core.OpLevel;

/**
 * TNT4J-Streams "Syslog" module constants.
 *
 * @version $Revision: 1 $
 */
public class SyslogStreamConstants {

	/**
	 * Resource bundle name constant for TNT4J-Streams "syslogd" module.
	 */
	public static final String RESOURCE_BUNDLE_NAME = "tnt4j-streams-syslogd"; // NON-NLS

	/**
	 * Log levels list in Syslog matching order.
	 */
	public static final OpLevel[] LEVELS = { OpLevel.HALT, // emergency
			OpLevel.FATAL, // alert
			OpLevel.CRITICAL, // critical
			OpLevel.ERROR, // error
			OpLevel.WARNING, // warning
			OpLevel.NOTICE, // notice
			OpLevel.INFO, // info
			OpLevel.DEBUG, // debug
			OpLevel.NONE };

	/**
	 * Constant for default Syslog facility 'USER'.
	 */
	public static final int DEFAULT_FACILITY = SyslogUtility.getFacility("user");
	/**
	 * Constant for default Syslog entry level 'INFO'.
	 */
	public static final int DEFAULT_LEVEL = 6;

	/**
	 * Constant for name of built-in Syslog property field map {@value} for structured Syslog data.
	 */
	public static final String FIELD_SYSLOG_MAP = "SyslogMap"; // NON-NLS
	/**
	 * Constant for name of built-in Syslog property field map {@value} for Syslog message contained variables.
	 */
	public static final String FIELD_SYSLOG_VARS = "SyslogVars"; // NON-NLS

	/**
	 * Constant for name of built-in Syslog property field {@value}.
	 */
	public static final String FIELD_FACILITY = "facility"; // NON-NLS
	/**
	 * Constant for name of built-in Syslog property field {@value}.
	 */
	public static final String FIELD_LEVEL = "level"; // NON-NLS
	/**
	 * Constant for name of built-in Syslog property field {@value}.
	 */
	public static final String FIELD_HOSTNAME = "hostname"; // NON-NLS
	/**
	 * Constant for name of built-in Syslog property field {@value}.
	 */
	public static final String FIELD_HOSTADDR = "hostaddr"; // NON-NLS
	/**
	 * Constant for name of built-in Syslog property field {@value}.
	 */
	public static final String FIELD_VERSION = "version"; // NON-NLS
	/**
	 * Constant for name of built-in Syslog property field {@value}.
	 */
	public static final String FIELD_PRIORITY = "priority"; // NON-NLS

	/**
	 * Constant for string value {@value}.
	 */
	public static final String UNKNOWN = "unknown"; // NON-NLS

	/**
	 * Constant for SPACE character {@value}.
	 */
	public static final char SPACE = ' ';
	/**
	 * Constant for MINUS character {@value}.
	 */
	public static final char MINUS = '-';
	/**
	 * Constant for PLUS character {@value}.
	 */
	public static final char PLUS = '+';
	/**
	 * Constant for COLON character {@value}.
	 */
	public static final char COLON = ':';
	/**
	 * Constant for QUOTE character {@value}.
	 */
	public static final char QUOTE = '"';
	/**
	 * Constant for NL character {@value}.
	 */
	public static final char NL = '\n';
	/**
	 * Constant for RC character {@value}.
	 */
	public static final char RC = '\r';
	/**
	 * Constant for ZERO character {@value}.
	 */
	public static final char ZERO = '0';
	/**
	 * Constant for OB character {@value}.
	 */
	public static final char OB = '[';
	/**
	 * Constant for CB character {@value}.
	 */
	public static final char CB = ']';
	/**
	 * Constant for EQ character {@value}.
	 */
	public static final char EQ = '=';
	/**
	 * Constant for SLASH character {@value}.
	 */
	public static final char SLASH = '\\';
	/**
	 * Constant for LT character {@value}.
	 */
	public static final char LT = '<';
	/**
	 * Constant for GT character {@value}.
	 */
	public static final char GT = '>';
	/**
	 * Constant for TZ character {@value}.
	 */
	public static final char TZ = 'T';
	/**
	 * Constant for DOT character {@value}.
	 */
	public static final char DOT = '.';
	/**
	 * Constant for UTC character {@value}.
	 */
	public static final char UTC = 'Z';

	/**
	 * Constant defining structured data map entry for structure identifier.
	 */
	public static final String SYSLOG_STRUCT_ID = "struct.id"; // NON-NLS
}
