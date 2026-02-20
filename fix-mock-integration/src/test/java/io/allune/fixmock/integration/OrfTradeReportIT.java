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

import io.allune.fixmock.FixMock;
import io.allune.fixmock.FixMockServer;
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
 * Core trade reporting flow: submit TCR (MsgType=AE) → receive OREN acknowledgement.
 *
 * <p>Uses {@code tradeCaptureReport()} convenience method (Gap #1 resolved) and
 * {@code inbound()} filtering (Gap #7 resolved) for clean verification of trade reporting flows.
 */
public class OrfTradeReportIT {

	private FixMockServer server;
	private Initiator initiator;

	@BeforeEach
	void setUp() {
		server = null;
		initiator = null;
	}

	@AfterEach
	void tearDown() {
		if (initiator != null) initiator.stop();
		if (server != null) server.stop();
	}

	@Test
	void submit_trade_report_and_receive_oren_acknowledgement() throws Exception {
		int port = 9950;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();
		assertTrue(server.isRunning());

		// Stub: when TCR (AE) arrives with Symbol=AAPL, respond with OREN acknowledgement
		Message ack = orfAcknowledgement("RPT-001", "TRD-10001", EVENT_SOURCE_OREN, "AAPL");

		server.stubFor(when(
				tradeCaptureReport()
						.withTag(TAG_SYMBOL, equalTo("AAPL"))
		).willReturn(respondWith(ack)));

		// Send a Trade Capture Report
		Message tcr = newTradeReport("RPT-001", "AAPL", 1000, 150.25);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		// Await acknowledgement
		Message response = app.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(response, "Expected OREN acknowledgement");
		assertEquals("AE", response.getHeader().getString(35));
		assertEquals("AAPL", response.getString(TAG_SYMBOL));
		assertEquals(EVENT_SOURCE_OREN, response.getString(TAG_MESSAGE_EVENT_SOURCE));
		assertEquals("TRD-10001", response.getString(TAG_TRADE_ID));

		// Verify the server received exactly one TCR from the reporter.
		// Using inbound() to filter only received messages (both request and response share MsgType=AE)
		server.verify(exactly(1), within(Duration.ofSeconds(5)),
				tradeCaptureReport()
						.inbound()
						.withTag(TAG_TRADE_REPORT_ID, equalTo("RPT-001")));
	}

	@Test
	void submit_trade_report_matching_on_trade_report_trans_type() throws Exception {
		int port = 9951;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Stub: match on TradeReportTransType=0 (New)
		Message ack = orfAcknowledgement("RPT-002", "TRD-10002", EVENT_SOURCE_OREN, "MSFT");
		server.stubFor(when(
				tradeCaptureReport()
						.withTag(TAG_TRADE_REPORT_TRANS_TYPE, equalTo("0"))
						.withTag(TAG_SYMBOL, equalTo("MSFT"))
		).willReturn(respondWith(ack)));

		Message tcr = newTradeReport("RPT-002", "MSFT", 500, 420.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Message response = app.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(response, "Expected OREN acknowledgement for MSFT trade");
		assertEquals("TRD-10002", response.getString(TAG_TRADE_ID));
	}

	@Test
	void verify_no_trades_received_on_empty_journal() throws Exception {
		int port = 9952;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Don't send anything — just verify zero matches
		server.verify(exactly(0), tradeCaptureReport());
	}

	@Test
	void stub_different_responses_for_different_symbols() throws Exception {
		int port = 9953;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// AAPL stub
		Message aaplAck = orfAcknowledgement("RPT-A", "TRD-A", EVENT_SOURCE_OREN, "AAPL");
		server.stubFor(when(
				tradeCaptureReport()
						.withTag(TAG_SYMBOL, equalTo("AAPL"))
		).atPriority(1).willReturn(respondWith(aaplAck)));

		// MSFT stub
		Message msftAck = orfAcknowledgement("RPT-M", "TRD-M", EVENT_SOURCE_OREN, "MSFT");
		server.stubFor(when(
				tradeCaptureReport()
						.withTag(TAG_SYMBOL, equalTo("MSFT"))
		).atPriority(1).willReturn(respondWith(msftAck)));

		// Send AAPL trade
		Message aaplTcr = newTradeReport("RPT-A", "AAPL", 100, 175.50);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(aaplTcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Message response = app.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(response);
		assertEquals("TRD-A", response.getString(TAG_TRADE_ID));

		// Verify AAPL was received (using inbound() to avoid counting outbound AE response)
		server.verify(exactly(1),
				tradeCaptureReport()
						.inbound()
						.withTag(TAG_SYMBOL, equalTo("AAPL")));
	}
}
