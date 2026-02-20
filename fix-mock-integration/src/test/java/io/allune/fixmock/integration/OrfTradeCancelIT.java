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
 * Cancel flow: submit cancel TCR (TradeReportTransType=1) → receive ORCX confirmation.
 */
public class OrfTradeCancelIT {

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
	void cancel_trade_report_receives_orcx_confirmation() throws Exception {
		int port = 9957;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Stub: cancel TCR → ORCX acknowledgement
		Message cancelAck = orfAcknowledgement("RPT-CXL-001", "TRD-CXL-001", EVENT_SOURCE_ORCX, "TSLA");
		server.stubFor(when(
				tradeCaptureReport()
						.withTag(TAG_TRADE_REPORT_TRANS_TYPE, equalTo("1")) // Cancel
						.withTag(TAG_SYMBOL, equalTo("TSLA"))
		).willReturn(respondWith(cancelAck)));

		// Send cancel TCR
		Message cancelTcr = cancelTradeReport("RPT-CXL-001", "TSLA", 500, 250.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(cancelTcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Message response = app.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(response, "Expected ORCX cancel confirmation");
		assertEquals("AE", response.getHeader().getString(35));
		assertEquals(EVENT_SOURCE_ORCX, response.getString(TAG_MESSAGE_EVENT_SOURCE));
		assertEquals("TRD-CXL-001", response.getString(TAG_TRADE_ID));
	}

	@Test
	void verify_cancel_request_distinguished_from_new_submission() throws Exception {
		int port = 9958;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Stub for new trades → OREN
		Message newAck = orfAcknowledgement("RPT-NEW", "TRD-NEW", EVENT_SOURCE_OREN, "AMZN");
		server.stubFor(when(
				tradeCaptureReport()
						.withTag(TAG_TRADE_REPORT_TRANS_TYPE, equalTo("0"))
		).atPriority(1).willReturn(respondWith(newAck)));

		// Stub for cancel → ORCX
		Message cxlAck = orfAcknowledgement("RPT-CXL", "TRD-CXL", EVENT_SOURCE_ORCX, "AMZN");
		server.stubFor(when(
				tradeCaptureReport()
						.withTag(TAG_TRADE_REPORT_TRANS_TYPE, equalTo("1"))
		).atPriority(1).willReturn(respondWith(cxlAck)));

		// Send a cancel TCR — should match the cancel stub, not the new one
		Message cancelTcr = cancelTradeReport("RPT-CXL", "AMZN", 200, 180.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(cancelTcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Message response = app.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(response);
		assertEquals(EVENT_SOURCE_ORCX, response.getString(TAG_MESSAGE_EVENT_SOURCE));

		// Verify only cancel was received, no new submissions
		server.verify(exactly(1), within(Duration.ofSeconds(5)),
				tradeCaptureReport().withTag(TAG_TRADE_REPORT_TRANS_TYPE, equalTo("1")));
		server.verify(exactly(0),
				tradeCaptureReport().withTag(TAG_TRADE_REPORT_TRANS_TYPE, equalTo("0")));
	}
}
