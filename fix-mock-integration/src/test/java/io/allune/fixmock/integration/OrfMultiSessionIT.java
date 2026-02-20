/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2025-2026 the original author or authors.
 */
package io.allune.fixmock.integration;

import io.allune.fixmock.FixMockServer;
import io.allune.fixmock.FixMockServerOptions;
import io.allune.fixmock.SessionConfig;
import io.allune.fixmock.server.SimpleFixMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.Initiator;
import quickfix.Message;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static io.allune.fixmock.FixMock.*;
import static io.allune.fixmock.integration.FixInitiatorSupport.*;
import static io.allune.fixmock.integration.OrfMessages.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Multi-session with SubIDs: reporting party vs contra party sessions, session-scoped verification.
 *
 * <p>Gap #5 resolved: {@code SessionConfig.builder().senderSubId().targetSubId()} enables
 * SenderSubID(50) and TargetSubID(57) configuration for mock server sessions.
 */
public class OrfMultiSessionIT {

	private FixMockServer server;
	private Initiator initiator1;
	private Initiator initiator2;

	@BeforeEach
	void setUp() {
		server = null;
		initiator1 = null;
		initiator2 = null;
	}

	@AfterEach
	void tearDown() {
		if (initiator1 != null) initiator1.stop();
		if (initiator2 != null) initiator2.stop();
		if (server != null) server.stop();
	}

	@Test
	void two_reporting_sessions_with_different_comp_ids() throws Exception {
		int port1 = 9977;
		int port2 = 9978;

		// Configure mock with two sessions for different reporting parties
		FixMockServerOptions options = FixMockServerOptions.builder()
				.addSession(SessionConfig.builder()
						.beginString("FIX.4.4")
						.senderCompId("FINRA-1")
						.targetCompId("REPORTER-A")
						.port(port1)
						.role(FixMockServerOptions.Role.ACCEPTOR)
						.build())
				.addSession(SessionConfig.builder()
						.beginString("FIX.4.4")
						.senderCompId("FINRA-2")
						.targetCompId("REPORTER-B")
						.port(port2)
						.role(FixMockServerOptions.Role.ACCEPTOR)
						.build())
				.build();

		server = new SimpleFixMockServer(options);
		server.start();
		assertTrue(server.isRunning());

		// Stub: session 1 → OREN ack for AAPL
		Message ack1 = orfAcknowledgement("RPT-MS-001", "TRD-MS-001", EVENT_SOURCE_OREN, "AAPL");
		server.stubFor(when(
				tradeCaptureReport()
						.senderCompId("REPORTER-A")
		).willReturn(respondWith(ack1)));

		// Stub: session 2 → OREN ack for MSFT
		Message ack2 = orfAcknowledgement("RPT-MS-002", "TRD-MS-002", EVENT_SOURCE_OREN, "MSFT");
		server.stubFor(when(
				tradeCaptureReport()
						.senderCompId("REPORTER-B")
		).willReturn(respondWith(ack2)));

		// Start two initiators
		Message tcr1 = newTradeReport("RPT-MS-001", "AAPL", 500, 175.00);
		SendOnceAndCaptureApp app1 = new SendOnceAndCaptureApp(tcr1);
		initiator1 = startInitiator(app1, "REPORTER-A", "FINRA-1", port1);

		Message tcr2 = newTradeReport("RPT-MS-002", "MSFT", 300, 420.00);
		SendOnceAndCaptureApp app2 = new SendOnceAndCaptureApp(tcr2);
		initiator2 = startInitiator(app2, "REPORTER-B", "FINRA-2", port2);

		// Wait for both
		assertTrue(((SimpleFixMockServer) server).awaitLogon(Duration.ofSeconds(5)));

		Message resp1 = app1.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(resp1, "Session 1 should receive OREN ack");
		assertEquals("TRD-MS-001", resp1.getString(TAG_TRADE_ID));

		Message resp2 = app2.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(resp2, "Session 2 should receive OREN ack");
		assertEquals("TRD-MS-002", resp2.getString(TAG_TRADE_ID));

		// Session-scoped verification using inbound() instead of senderCompId workaround
		server.session("FIX.4.4:FINRA-1->REPORTER-A")
				.verify(exactly(1), tradeCaptureReport().inbound());
		server.session("FIX.4.4:FINRA-2->REPORTER-B")
				.verify(exactly(1), tradeCaptureReport().inbound());

		// Combined: 2 total inbound TCRs
		server.verify(exactly(2), tradeCaptureReport()
				.inbound()
				.withTag(TAG_TRADE_REPORT_TRANS_TYPE, equalTo("0")));
	}

	@Test
	void session_scoped_verification_isolates_messages() throws Exception {
		int port1 = 9979;
		int port2 = 9980;

		FixMockServerOptions options = FixMockServerOptions.builder()
				.addSession(SessionConfig.builder()
						.beginString("FIX.4.4")
						.senderCompId("FINRA-X")
						.targetCompId("FIRM-X")
						.port(port1)
						.role(FixMockServerOptions.Role.ACCEPTOR)
						.build())
				.addSession(SessionConfig.builder()
						.beginString("FIX.4.4")
						.senderCompId("FINRA-Y")
						.targetCompId("FIRM-Y")
						.port(port2)
						.role(FixMockServerOptions.Role.ACCEPTOR)
						.build())
				.build();

		server = new SimpleFixMockServer(options);
		server.start();

		// Only send on session 1
		Message tcr = newTradeReport("RPT-ISO-001", "TSLA", 100, 250.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator1 = startInitiator(app, "FIRM-X", "FINRA-X", port1);

		// Start session 2 but don't send anything
		CaptureOnlyApp app2 = new CaptureOnlyApp(0);
		initiator2 = startInitiator(app2, "FIRM-Y", "FINRA-Y", port2);

		assertTrue(((SimpleFixMockServer) server).awaitLogon(Duration.ofSeconds(5)));
		Thread.sleep(2000);

		// Session 1 should have 1 TCR
		server.session("FIX.4.4:FINRA-X->FIRM-X")
				.verify(exactly(1), tradeCaptureReport());

		// Session 2 should have 0 TCRs
		server.session("FIX.4.4:FINRA-Y->FIRM-Y")
				.verify(exactly(0), tradeCaptureReport());
	}

	@Test
	void session_config_supports_sub_ids() throws Exception {
		// Gap #5 resolved: SessionConfig now supports SenderSubID/TargetSubID
		int port = 9981;

		FixMockServerOptions options = FixMockServerOptions.builder()
				.addSession(SessionConfig.builder()
						.beginString("FIX.4.4")
						.senderCompId("FINRA")
						.senderSubId("FINRA-SUB")
						.targetCompId("REPORTER")
						.targetSubId("REPORTER-SUB")
						.port(port)
						.role(FixMockServerOptions.Role.ACCEPTOR)
						.build())
				.build();
		server = new SimpleFixMockServer(options);
		server.start();

		// Send a TCR using initiator configured with matching SubIDs
		Message tcr = newTradeReport("RPT-HDR-001", "AAPL", 100, 175.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator1 = startInitiator(app, "REPORTER", "REPORTER-SUB", "FINRA", "FINRA-SUB", port);

		Thread.sleep(2000);

		// Verify the message was received via the SubID-enabled session
		server.verify(exactly(1), within(Duration.ofSeconds(5)),
				tradeCaptureReport()
						.inbound()
						.senderCompId("REPORTER"));
	}
}
