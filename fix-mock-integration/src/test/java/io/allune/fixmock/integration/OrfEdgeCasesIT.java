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
import io.allune.fixmock.verify.FixJournalEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.Initiator;
import quickfix.Message;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.allune.fixmock.FixMock.*;
import static io.allune.fixmock.integration.FixInitiatorSupport.*;
import static io.allune.fixmock.integration.OrfMessages.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge cases: empty journal, no stubs matched, verify with zero matches,
 * large messages, timing. Uses {@code inbound()} filtering (Gap #7 resolved)
 * instead of {@code senderCompId()} workaround.
 */
public class OrfEdgeCasesIT {

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
	void empty_journal_verify_zero_matches() throws Exception {
		int port = 9982;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// No initiator connected, no messages sent
		server.verify(exactly(0), tradeCaptureReport());
		server.verify(exactly(0), tradeCaptureReportAck());
		server.verify(exactly(0), messageType("D"));
	}

	@Test
	void no_stub_matched_message_still_journalled() throws Exception {
		int port = 9983;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// No stubs registered — message should still be received and journalled
		Message tcr = newTradeReport("RPT-EDGE-001", "AAPL", 100, 150.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		// No response expected (no stubs)
		Message response = app.awaitFirstResponse(2, TimeUnit.SECONDS);
		assertNull(response, "No stub registered, so no response expected");

		// But the message IS in the journal
		server.verify(exactly(1), within(Duration.ofSeconds(3)),
				tradeCaptureReport().withTag(TAG_TRADE_REPORT_ID, equalTo("RPT-EDGE-001")));
	}

	@Test
	void verification_failure_produces_readable_error() throws Exception {
		// Gap #7: Test that verification failure with a complex TCR produces a useful error message
		int port = 9984;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		Message tcr = newTradeReportWithFinraTags("RPT-EDGE-002", "GOOG", 500, 2800.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Thread.sleep(2000);

		// This verification should PASS (using inbound() instead of senderCompId workaround)
		server.verify(exactly(1), within(Duration.ofSeconds(3)),
				tradeCaptureReport()
						.inbound()
						.withTag(TAG_SYMBOL, equalTo("GOOG")));

		// This verification should FAIL — wrong symbol
		AssertionError verificationError = assertThrows(AssertionError.class, () ->
				server.verify(exactly(1),
						tradeCaptureReport()
								.inbound()
								.withTag(TAG_SYMBOL, equalTo("WRONG_SYMBOL")))
		);
		assertNotNull(verificationError.getMessage());
		// The error message should include the pattern description
		assertTrue(verificationError.getMessage().length() > 0,
				"Error message should be non-empty and descriptive");
	}

	@Test
	void large_message_with_many_tags_handled_correctly() throws Exception {
		int port = 9985;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Stub response for large message
		Message ack = orfAcknowledgement("RPT-LARGE", "TRD-LARGE", EVENT_SOURCE_OREN, "STRESS");
		server.stubFor(when(
				tradeCaptureReport()
						.withTag(TAG_TRADE_REPORT_ID, equalTo("RPT-LARGE"))
		).willReturn(respondWith(ack)));

		// Send a large TCR with many tags and multiple party groups
		Message largeTcr = largeTradeReport("RPT-LARGE", "STRESS");
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(largeTcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Message response = app.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(response, "Large message should be processed and response received");
		assertEquals("TRD-LARGE", response.getString(TAG_TRADE_ID));

		// Verify the large message was received (using inbound() to exclude outbound AE ack)
		server.verify(exactly(1), within(Duration.ofSeconds(5)),
				tradeCaptureReport()
						.inbound()
						.withTag(TAG_TRADE_REPORT_ID, equalTo("RPT-LARGE")));
	}

	@Test
	void journal_snapshot_contains_orf_messages() throws Exception {
		int port = 9986;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		Message tcr = newTradeReport("RPT-SNAP-001", "TSLA", 200, 250.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Thread.sleep(2000);

		// Access journal snapshot
		List<FixJournalEntry> journal = server.getJournalSnapshot();
		assertFalse(journal.isEmpty(), "Journal should not be empty after sending a message");

		// Find the AE message in the journal
		boolean foundTcr = journal.stream()
				.anyMatch(entry -> "AE".equals(entry.getMsgType())
						&& "RPT-SNAP-001".equals(entry.getTags().get(TAG_TRADE_REPORT_ID)));
		assertTrue(foundTcr, "Journal should contain the TCR with TradeReportID=RPT-SNAP-001");
	}

	@Test
	void clear_journal_and_verify_empty() throws Exception {
		int port = 9987;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		Message tcr = newTradeReport("RPT-CLR-001", "AMZN", 100, 3400.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Thread.sleep(2000);

		// Verify TCR was received
		server.verify(atLeast(1), within(Duration.ofSeconds(3)), tradeCaptureReport());

		// Clear journal
		int cleared = server.clearJournal();
		assertTrue(cleared > 0, "Should have cleared at least one journal entry");

		// After clearing, verify zero matches
		server.verify(exactly(0), tradeCaptureReport());
	}

	@Test
	void clear_stubs_stops_matching() throws Exception {
		int port = 9988;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Register a stub
		Message ack = orfAcknowledgement("RPT-STUB", "TRD-STUB", EVENT_SOURCE_OREN, "AAPL");
		server.stubFor(when(
				tradeCaptureReport()
		).willReturn(respondWith(ack)));

		// Clear all stubs
		int cleared = server.clearStubs();
		assertTrue(cleared > 0, "Should have cleared at least one stub");

		// Send a TCR — should NOT get a response since stubs were cleared
		Message tcr = newTradeReport("RPT-STUB", "AAPL", 100, 175.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Message response = app.awaitFirstResponse(2, TimeUnit.SECONDS);
		assertNull(response, "No response expected after stubs cleared");
	}

	@Test
	void timing_window_verification_for_orf_messages() throws Exception {
		int port = 9989;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		Message tcr = newTradeReport("RPT-TIME-001", "META", 50, 340.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		// Allow time for logon and message exchange
		Thread.sleep(2000);

		// Verify within a time window — message should have arrived (using inbound())
		server.verify(atLeast(1), within(Duration.ofSeconds(5)),
				tradeCaptureReport()
						.inbound()
						.withTag(TAG_SYMBOL, equalTo("META")));
	}

}
