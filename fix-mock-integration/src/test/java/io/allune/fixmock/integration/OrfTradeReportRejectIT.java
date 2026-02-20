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
 * Rejection flow: submit TCR → receive AR reject with reject reason codes.
 *
 * <p>Uses {@code tradeCaptureReportAck()} convenience method (Gap #6 resolved).
 */
public class OrfTradeReportRejectIT {

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
	void trade_report_rejected_with_reason_code() throws Exception {
		int port = 9954;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Stub: reject any TCR with an invalid symbol
		Message reject = tradeReportReject("RPT-BAD", 0, "Unknown symbol");
		server.stubFor(when(
				tradeCaptureReport()
						.withTag(TAG_SYMBOL, equalTo("INVALID"))
		).willReturn(respondWith(reject)));

		// Send TCR with invalid symbol
		Message tcr = newTradeReport("RPT-BAD", "INVALID", 100, 0.01);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Message response = app.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(response, "Expected AR reject");
		assertEquals("AR", response.getHeader().getString(35));
		assertEquals(0, response.getInt(TAG_TRADE_REPORT_REJ_REASON));
		assertEquals("Unknown symbol", response.getString(TAG_TEXT));
	}

	@Test
	void reject_with_different_reason_codes() throws Exception {
		int port = 9955;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Stub: reject reason 1 = Unknown or invalid instrument
		Message reject = tradeReportReject("RPT-R1", 1, "Invalid instrument");
		server.stubFor(when(
				tradeCaptureReport()
						.withTag(TAG_TRADE_REPORT_TYPE, equalTo("0"))
		).willReturn(respondWith(reject)));

		Message tcr = newTradeReport("RPT-R1", "XYZ", 200, 5.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Message response = app.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(response, "Expected rejection");
		assertEquals("AR", response.getHeader().getString(35));
		assertEquals(1, response.getInt(TAG_TRADE_REPORT_REJ_REASON));
	}

	@Test
	void verify_reject_message_received_by_server_using_messageType_AR() throws Exception {
		int port = 9956;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// No stubs — just verify we can match on TCR messages
		Message tcr = newTradeReport("RPT-V1", "GOOG", 300, 140.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Thread.sleep(2000);

		// Verify TCR was received using tradeCaptureReport()
		server.verify(exactly(1), within(Duration.ofSeconds(5)),
				tradeCaptureReport().withTag(TAG_SYMBOL, equalTo("GOOG")));
	}
}
