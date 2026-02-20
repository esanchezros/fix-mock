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
 * Header-level matching for ORF messages: SenderCompID, TargetCompID, and MsgType in the header.
 *
 * <p>Tests that the stubbing and verification DSL correctly handles header fields for
 * trade reporting messages, complementing the multi-session tests in {@link OrfMultiSessionIT}.
 */
public class OrfHeaderMatchingIT {

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
	void stub_matches_on_sender_comp_id_in_header() throws Exception {
		int port = 9990;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "MPID-ABC", port);
		server.start();

		// Stub: match on SenderCompID(49)=MPID-ABC in header
		Message ack = orfAcknowledgement("RPT-HDR-001", "TRD-HDR-001", EVENT_SOURCE_OREN, "AAPL");
		server.stubFor(when(
				appMessageRequestedFor()
						.senderCompId("MPID-ABC")
						.messageType("AE")
		).willReturn(respondWith(ack)));

		Message tcr = newTradeReport("RPT-HDR-001", "AAPL", 100, 175.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "MPID-ABC", "FINRA", port);

		Message response = app.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(response, "Should match on SenderCompID header field");
		assertEquals("TRD-HDR-001", response.getString(TAG_TRADE_ID));
	}

	@Test
	void verify_with_combined_header_and_body_criteria() throws Exception {
		int port = 9991;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "MPID-XYZ", port);
		server.start();

		Message tcr = newTradeReportWithFinraTags("RPT-COMBO-001", "TSLA", 200, 250.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "MPID-XYZ", "FINRA", port);

		Thread.sleep(2000);

		// Verify combining header and body criteria
		server.verify(exactly(1), within(Duration.ofSeconds(5)),
				appMessageRequestedFor()
						.senderCompId("MPID-XYZ")
						.messageType("AE")
						.withTag(TAG_SYMBOL, equalTo("TSLA"))
						.withTag(TAG_REPORTING_OBLIGATION, equalTo("Y")));
	}

	@Test
	void header_field_mismatch_prevents_stub_match() throws Exception {
		int port = 9992;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Stub that requires SenderCompID=WRONG-MPID (won't match actual sender)
		Message ack = orfAcknowledgement("RPT-MISS", "TRD-MISS", EVENT_SOURCE_OREN, "AAPL");
		server.stubFor(when(
				appMessageRequestedFor()
						.senderCompId("WRONG-MPID")
						.messageType("AE")
		).willReturn(respondWith(ack)));

		Message tcr = newTradeReport("RPT-MISS", "AAPL", 100, 175.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		// No response because SenderCompID doesn't match the stub
		Message response = app.awaitFirstResponse(2, TimeUnit.SECONDS);
		assertNull(response, "Stub should NOT match when SenderCompID differs");

		// But the message is still journalled
		server.verify(exactly(1), within(Duration.ofSeconds(3)),
				messageType("AE").withTag(TAG_TRADE_REPORT_ID, equalTo("RPT-MISS")));
	}
}
