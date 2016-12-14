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

package com.jkoolcloud.tnt4j.streams.utils;

import com.jkoolcloud.tnt4j.core.OpLevel;

/**
 * TNT4J-Streams "Syslog" module constants.
 *
 * @version $Revision: 1 $
 */
public class SyslogStreamConstants {

	/**
	 * Resource bundle name constant for TNT4J-Streams "syslog" module.
	 */
	public static final String RESOURCE_BUNDLE_NAME = "tnt4j-streams-syslog"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_PROTOCOL = "Protocol"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_TIMEOUT = "Timeout"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_FACILITY = "Facility"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_LEVEL = "Level"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_CHAR_SET = "CharSet"; // NON-NLS

	/**
	 * Log facilities enum in syslog order.
	 */
	public enum Facility {
		KERN, USER, MAIL, DAEMON, AUTH, SYSLOG, LPR, NEWS, UUCP, CRON, AUTHPRIV, FTP, NTP, AUDIT, ALERT, CLOCK, LOCAL0, LOCAL1, LOCAL2, LOCAL3, LOCAL4, LOCAL5, LOCAL6, LOCAL7, UNKNOWN;

		/**
		 * Returns enumeration member matching defined ordinal.
		 *
		 * @param ordinal
		 *            enumeration member ordinal
		 * @return enumeration member or {@code UNKNOWN} if defined ordinal is out of enumeration values range
		 */
		public static Facility valueOf(int ordinal) {
			Facility[] facilities = values();
			return ((ordinal >= 0) && (ordinal < facilities.length)) ? facilities[ordinal]
					: facilities[facilities.length - 1];
		}
	}

	/**
	 * Log levels list in syslog matching order.
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

	public static final int DEFAULT_FACILITY = Facility.USER.ordinal();
	public static final int DEFAULT_LEVEL = 6;

	/**
	 * Constant for name of built-in syslog property field map {@value} for structured syslog data.
	 */
	public static String FIELD_SYSLOG_MAP = "SyslogMap"; // NON-NLS
	/**
	 * Constant for name of built-in syslog property field map {@value} for syslog message contained variables.
	 */
	public static String FIELD_SYSLOG_VARS = "SyslogVars"; // NON-NLS

	/**
	 * Constant for name of built-in syslog property field {@value}.
	 */
	public static final String FIELD_FACILITY = "facility"; // NON-NLS
	/**
	 * Constant for name of built-in syslog property field {@value}.
	 */
	public static final String FIELD_LEVEL = "level"; // NON-NLS
	/**
	 * Constant for name of built-in syslog property field {@value}.
	 */
	public static final String FIELD_HOSTNAME = "hostname"; // NON-NLS
	/**
	 * Constant for name of built-in syslog property field {@value}.
	 */
	public static final String FIELD_HOSTADDR = "hostaddr"; // NON-NLS
	/**
	 * Constant for name of built-in syslog property field {@value}.
	 */
	public static final String FIELD_VERSION = "version"; // NON-NLS
	/**
	 * Constant for name of built-in syslog property field {@value}.
	 */
	public static final String FIELD_PRIORITY = "priority"; // NON-NLS

	public static final String UNKNOWN = "unknown"; // NON-NLS

	public static final char SPACE = ' ';
	public static final char MINUS = '-';
	public static final char COLON = ':';
	public static final char QUOTE = '"';
	public static final char NL = '\n';
	public static final char RC = '\r';
	public static final char ZERO = '0';
	public static final char OB = '[';
	public static final char CB = ']';
	public static final char EQ = '=';
	public static final char SLASH = '\\';
}
