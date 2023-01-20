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

import static org.junit.Assert.assertFalse;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.graylog2.syslog4j.server.impl.event.structured.StructuredSyslogServerEvent;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.SyslogParserProperties;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivitySyslogEventParserTest {
	@Test
	public void eventFromMessageTest() throws Exception {
		String msg = "<194>1 2018-05-07T14:36:54.199084Z SVSCPLEX-S0W1 CICSTS51 STC03822 HASP893 [zXpert@1796 SYSID=\"S0W1\" TYPE=\"CONSOLE\" SUBTYPE=\"JES2\" RESOURCE=\"HASP893\" MSG_DATE=\"18122\" MSG_TIME=\"14.35.16\" SEQID=\"55\" SEQNO=\"00015547\" ASIDX=\"001E\" MSGTYPE=\"WTO\"]    $HASP893 VOLUME(VPSPOL) STATUS=ACTIVE,PERCENT=100";
		StructuredSyslogServerEvent evt = new StructuredSyslogServerEvent(msg, InetAddress.getByName("172.16.6.86"));

		Map<String, String> props = new HashMap<>(1);
		props.put(SyslogParserProperties.PROP_FLATTEN_STRUCTURED_DATA, "true");

		ActivitySyslogEventParser eParser = new ActivitySyslogEventParser();
		eParser.setProperties(props.entrySet());
		Map<?, ?> eDataMap = eParser.getDataMap(evt);

		assertFalse(MapUtils.isEmpty(eDataMap));

		ActivitySyslogLineParser lParser = new ActivitySyslogLineParser();
		lParser.setProperties(props.entrySet());
		Map<?, ?> lDataMap = lParser.getDataMap(msg);

		assertFalse(MapUtils.isEmpty(lDataMap));
	}

	@Test
	public void eventFromMessageTest2() throws Exception {
		// String msg = "<190>1 2019-03-18T16:04:47.496592Z SVSCPLEX-S0W1 JES2 - $HASP603 [zXpert@1796 SYSID=\"S0W1\"
		// TYPE=\"CONSOLE\" SUBTYPE=\"JES2\" RESOURCE=\"$HASP603\" MSG_DESC=\"SYSSTATUS\" MSG_DATE=\"19077\"
		// MSG_TIME=\"11.04.47\" SEQID=\"78\" SEQNO=\"00017017\" ASIDX=\"001F\" MSGTYPE=\"WTO\"] ∩╗┐$HASP603 RDR1
		// $HASP603 RDR1 UNIT=000A,STATUS=DRAINED,AUTH=(DEVICE=YES, $HASP603
		// JOB=YES,SYSTEM=YES),CLASS=A,HOLD=NO,MSGCLASS=A, $HASP603 PRIOINC=1,PRIOLIM=15,PRTDEST=LOCAL,PUNDEST=LOCAL,
		// $HASP603 SYSAFF=(ANY),TRACE=NO,XEQDEST=LOCAL  ccc(yyy_) rerer=   \"rerere\"  rrr  () afaf";
		String msg = "<190>1 2019-03-18T16:04:47.496592Z SVSCPLEX-S0W1 JES2 - $HASP603 [zXpert@1796 SYSID=\"S0W1\" TYPE=\"CONSOLE\" SUBTYPE=\"JES2\" RESOURCE=\"$HASP603\" MSG_DESC=\"SYSSTATUS\" MSG_DATE=\"19077\" MSG_TIME=\"11.04.47\" SEQID=\"78\" SEQNO=\"00017017\" ASIDX=\"001F\" MSGTYPE=\"WTO\"] ∩╗┐$HASP603 RDR1 $HASP603 RDR1   UNIT=000A,STATUS=DRAINED,AUTH=(DEVICE=YES, $HASP603        JOB=YES,SYSTEM=YES),CLASS=A,HOLD=NO,MSGCLASS=A, $HASP603        PRIOINC=1,PRIOLIM=15,PRTDEST=LOCAL,PUNDEST=LOCAL, $HASP603        SYSAFF=(ANY),TRACE=NO,XEQDEST=LOCAL    ccc(yyy_)     rerer=      \"rerere\"          rrr() afaf";

		StructuredSyslogServerEvent evt = new StructuredSyslogServerEvent(msg, InetAddress.getByName("172.16.6.86"));

		Map<String, String> props = new HashMap<>(1);
		props.put(SyslogParserProperties.PROP_FLATTEN_STRUCTURED_DATA, "true");

		ActivitySyslogEventParser eParser = new ActivitySyslogEventParser();
		eParser.setProperties(props.entrySet());
		Map<?, ?> eDataMap = eParser.getDataMap(evt);

		assertFalse(MapUtils.isEmpty(eDataMap));

		ActivitySyslogLineParser lParser = new ActivitySyslogLineParser();
		lParser.setProperties(props.entrySet());
		Map<?, ?> lDataMap = lParser.getDataMap(msg);

		assertFalse(MapUtils.isEmpty(lDataMap));
	}
}
