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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.allune.fixmock.FixMock.*;
import static io.allune.fixmock.integration.FixInitiatorSupport.*;
import static io.allune.fixmock.integration.OrfMessages.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Full lifecycle with scenarios: Report → Ack → Cancel → Confirm Cancel.
 *
 * <p>Uses the FixMock scenario DSL (inScenario/whenScenarioStateIs/willSetStateTo) to model
 * the stateful progression of a trade through the ORF system.
 */
public class OrfTradeLifecycleIT {

	private static final String SCENARIO_NAME = "orf-trade-lifecycle";
	private static final String STATE_STARTED = "Started";
	private static final String STATE_REPORTED = "Reported";
	private static final String STATE_CANCELLED = "Cancelled";

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
	void full_lifecycle_report_then_cancel_with_scenario_states() throws Exception {
		int port = 9959;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Phase 1: New trade → OREN acknowledgement, transition to "Reported" state
		Message orenAck = orfAcknowledgement("RPT-LC-001", "TRD-LC-001", EVENT_SOURCE_OREN, "NVDA");
		server.stubFor(when(
				appMessageRequestedFor()
						.messageType("AE")
						.withTag(TAG_TRADE_REPORT_TRANS_TYPE, equalTo("0"))
		).inScenario(SCENARIO_NAME)
				.whenScenarioStateIs(STATE_STARTED)
				.willSetStateTo(STATE_REPORTED)
				.willReturn(respondWith(orenAck)));

		// Phase 2: Cancel trade → ORCX acknowledgement, transition to "Cancelled" state
		Message orcxAck = orfAcknowledgement("RPT-LC-002", "TRD-LC-001", EVENT_SOURCE_ORCX, "NVDA");
		server.stubFor(when(
				appMessageRequestedFor()
						.messageType("AE")
						.withTag(TAG_TRADE_REPORT_TRANS_TYPE, equalTo("1"))
		).inScenario(SCENARIO_NAME)
				.whenScenarioStateIs(STATE_REPORTED)
				.willSetStateTo(STATE_CANCELLED)
				.willReturn(respondWith(orcxAck)));

		// Send both messages: new trade report then cancel
		Message newTcr = newTradeReport("RPT-LC-001", "NVDA", 1000, 890.50);
		Message cancelTcr = cancelTradeReport("RPT-LC-002", "NVDA", 1000, 890.50);

		SendMultipleAndCaptureApp app = new SendMultipleAndCaptureApp(List.of(newTcr, cancelTcr), 2);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		List<Message> responses = app.awaitResponses(5, TimeUnit.SECONDS);
		assertEquals(2, responses.size(), "Expected 2 responses: OREN + ORCX");

		// First response: OREN (new trade acknowledged)
		assertEquals("AE", responses.get(0).getHeader().getString(35));
		assertEquals(EVENT_SOURCE_OREN, responses.get(0).getString(TAG_MESSAGE_EVENT_SOURCE));

		// Second response: ORCX (cancel confirmed)
		assertEquals("AE", responses.get(1).getHeader().getString(35));
		assertEquals(EVENT_SOURCE_ORCX, responses.get(1).getString(TAG_MESSAGE_EVENT_SOURCE));

		// Verify both inbound TCRs were received (filter by senderCompId to exclude outbound AE acks)
		server.verify(exactly(2), within(Duration.ofSeconds(5)),
				appMessageRequestedFor().senderCompId("REPORTER").messageType("AE"));
	}

	@Test
	void scenario_state_prevents_cancel_before_report() throws Exception {
		int port = 9960;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Only register the cancel stub (requires "Reported" state) — no new trade stub
		Message orcxAck = orfAcknowledgement("RPT-CXL-EARLY", "TRD-NONE", EVENT_SOURCE_ORCX, "META");
		server.stubFor(when(
				appMessageRequestedFor()
						.messageType("AE")
						.withTag(TAG_TRADE_REPORT_TRANS_TYPE, equalTo("1"))
		).inScenario(SCENARIO_NAME)
				.whenScenarioStateIs(STATE_REPORTED) // Requires Reported state
				.willReturn(respondWith(orcxAck)));

		// Send a cancel TCR without first reporting — should NOT match (scenario is in Started state)
		Message cancelTcr = cancelTradeReport("RPT-CXL-EARLY", "META", 100, 500.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(cancelTcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		// No response expected since scenario state doesn't match
		Message response = app.awaitFirstResponse(2, TimeUnit.SECONDS);
		assertNull(response, "Should not receive ORCX when scenario state is not 'Reported'");

		// Verify the TCR was still received (just not matched by any stub)
		server.verify(exactly(1), within(Duration.ofSeconds(3)), messageType("AE"));
	}

	@Test
	void sequence_based_lifecycle_without_scenarios() throws Exception {
		int port = 9961;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Alternative approach: use sequence() for ordered responses
		Message orenAck = orfAcknowledgement("RPT-SEQ-1", "TRD-SEQ-1", EVENT_SOURCE_OREN, "AMD");
		Message orcxAck = orfAcknowledgement("RPT-SEQ-2", "TRD-SEQ-1", EVENT_SOURCE_ORCX, "AMD");

		server.stubFor(when(
				appMessageRequestedFor().messageType("AE")
		).willReturn(FixMock.sequence(
				respondWith(orenAck),
				respondWith(orcxAck)
		)));

		// Send two TCRs
		Message tcr1 = newTradeReport("RPT-SEQ-1", "AMD", 500, 160.00);
		Message tcr2 = cancelTradeReport("RPT-SEQ-2", "AMD", 500, 160.00);

		SendMultipleAndCaptureApp app = new SendMultipleAndCaptureApp(List.of(tcr1, tcr2), 2);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		List<Message> responses = app.awaitResponses(5, TimeUnit.SECONDS);
		assertEquals(2, responses.size());
		assertEquals(EVENT_SOURCE_OREN, responses.get(0).getString(TAG_MESSAGE_EVENT_SOURCE));
		assertEquals(EVENT_SOURCE_ORCX, responses.get(1).getString(TAG_MESSAGE_EVENT_SOURCE));
	}
}
