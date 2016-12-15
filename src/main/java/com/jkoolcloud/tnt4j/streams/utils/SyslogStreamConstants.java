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
	 * Constant for name of built-in stream {@value} property.
	 */
	public static final String PROP_PROTOCOL = "Protocol"; // NON-NLS

	/**
	 * Constant for name of built-in stream {@value} property.
	 */
	public static final String PROP_TIMEOUT = "Timeout"; // NON-NLS

	/**
	 * Constant for name of built-in stream {@value} property.
	 */
	public static final String PROP_FACILITY = "Facility"; // NON-NLS

	/**
	 * Constant for name of built-in stream {@value} property.
	 */
	public static final String PROP_LEVEL = "Level"; // NON-NLS

	/**
	 * Constant for name of built-in parser {@value} property.
	 */
	public static final String PROP_CHAR_SET = "CharSet"; // NON-NLS

	/**
	 * Log facilities enum in syslog order.
	 */
	public enum Facility {
		/**
		 * Kern facility.
		 */
		KERN,
		/**
		 * User facility.
		 */
		USER,
		/**
		 * Mail facility.
		 */
		MAIL,
		/**
		 * Daemon facility.
		 */
		DAEMON,
		/**
		 * Auth facility.
		 */
		AUTH,
		/**
		 * Syslog facility.
		 */
		SYSLOG,
		/**
		 * Lpr facility.
		 */
		LPR,
		/**
		 * News facility.
		 */
		NEWS,
		/**
		 * Uucp facility.
		 */
		UUCP,
		/**
		 * Cron facility.
		 */
		CRON,
		/**
		 * Authpriv facility.
		 */
		AUTHPRIV,
		/**
		 * Ftp facility.
		 */
		FTP,
		/**
		 * Ntp facility.
		 */
		NTP,
		/**
		 * Audit facility.
		 */
		AUDIT,
		/**
		 * Alert facility.
		 */
		ALERT,
		/**
		 * Clock facility.
		 */
		CLOCK,
		/**
		 * Local 0 facility.
		 */
		LOCAL0,
		/**
		 * Local 1 facility.
		 */
		LOCAL1,
		/**
		 * Local 2 facility.
		 */
		LOCAL2,
		/**
		 * Local 3 facility.
		 */
		LOCAL3,
		/**
		 * Local 4 facility.
		 */
		LOCAL4,
		/**
		 * Local 5 facility.
		 */
		LOCAL5,
		/**
		 * Local 6 facility.
		 */
		LOCAL6,
		/**
		 * Local 7 facility.
		 */
		LOCAL7,
		/**
		 * Unknown facility.
		 */
		UNKNOWN;

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

	/**
	 * Constant for default syslog facility 'USER'.
	 */
	public static final int DEFAULT_FACILITY = Facility.USER.ordinal();
	/**
	 * Constant for default syslog entry level 'INFO'.
	 */
	public static final int DEFAULT_LEVEL = 6;

	/**
	 * Constant for name of built-in syslog property field map {@value} for structured syslog data.
	 */
	public static final String FIELD_SYSLOG_MAP = "SyslogMap"; // NON-NLS
	/**
	 * Constant for name of built-in syslog property field map {@value} for syslog message contained variables.
	 */
	public static final String FIELD_SYSLOG_VARS = "SyslogVars"; // NON-NLS

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

	/**
	 * Constant for string value '{@value}'.
	 */
	public static final String UNKNOWN = "unknown"; // NON-NLS

	/**
	 * Constant for SPACE character '{@value}'.
	 */
	public static final char SPACE = ' ';
	/**
	 * Constant for MINUS character '{@value}'.
	 */
	public static final char MINUS = '-';
	/**
	 * Constant for PLUS character '{@value}'.
	 */
	public static final char PLUS = '+';
	/**
	 * Constant for COLON character '{@value}'.
	 */
	public static final char COLON = ':';
	/**
	 * Constant for QUOTE character '{@value}'.
	 */
	public static final char QUOTE = '"';
	/**
	 * Constant for NL character '{@value}'.
	 */
	public static final char NL = '\n';
	/**
	 * Constant for RC character '{@value}'.
	 */
	public static final char RC = '\r';
	/**
	 * Constant for ZERO character '{@value}'.
	 */
	public static final char ZERO = '0';
	/**
	 * Constant for OB character '{@value}'.
	 */
	public static final char OB = '[';
	/**
	 * Constant for CB character '{@value}'.
	 */
	public static final char CB = ']';
	/**
	 * Constant for EQ character '{@value}'.
	 */
	public static final char EQ = '=';
	/**
	 * Constant for SLASH character '{@value}'.
	 */
	public static final char SLASH = '\\';
	/**
	 * Constant for LT character '{@value}'.
	 */
	public static final char LT = '<';
	/**
	 * Constant for GT character '{@value}'.
	 */
	public static final char GT = '>';
	/**
	 * Constant for TZ character '{@value}'.
	 */
	public static final char TZ = 'T';
	/**
	 * Constant for DOT character '{@value}'.
	 */
	public static final char DOT = '.';
	/**
	 * Constant for UTC character '{@value}'.
	 */
	public static final char UTC = 'Z';
}
