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
import io.allune.fixmock.matching.TagResolver;
import io.allune.fixmock.server.SimpleFixMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.Initiator;
import quickfix.Message;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.allune.fixmock.FixMock.*;
import static io.allune.fixmock.integration.FixInitiatorSupport.*;
import static io.allune.fixmock.integration.OrfMessages.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Custom tag matching: verify FINRA extension tags (22xxx range) work with both
 * {@code withTag(int, ...)} and {@code withTagName(String, ...)} via {@code TagResolver.registerAlias()}.
 *
 * <p>Gap #2 resolved: Users can register custom tag name aliases for domain-specific extensions.
 */
public class OrfCustomTagsIT {

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
	void match_on_finra_custom_tag_by_number() throws Exception {
		int port = 9962;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Stub: match on FINRA custom tag ReportingObligation (22030) by number
		Message ack = orfAcknowledgement("RPT-CT-001", "TRD-CT-001", EVENT_SOURCE_OREN, "AAPL");
		server.stubFor(when(
				tradeCaptureReport()
						.withTag(TAG_REPORTING_OBLIGATION, equalTo("Y"))
		).willReturn(respondWith(ack)));

		// Send TCR with ReportingObligation=Y
		Message tcr = newTradeReportWithFinraTags("RPT-CT-001", "AAPL", 1000, 150.25);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Message response = app.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(response, "Should match on custom FINRA tag 22030 by number");
		assertEquals("TRD-CT-001", response.getString(TAG_TRADE_ID));

		// Verify using custom tag number
		server.verify(exactly(1), within(Duration.ofSeconds(5)),
				tradeCaptureReport().withTag(TAG_REPORTING_OBLIGATION, equalTo("Y")));
	}

	@Test
	void match_on_multiple_custom_tags_simultaneously() throws Exception {
		int port = 9963;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		Message ack = orfAcknowledgement("RPT-CT-002", "TRD-CT-002", EVENT_SOURCE_OREN, "GOOG");
		server.stubFor(when(
				tradeCaptureReport()
						.withTag(TAG_SYMBOL, equalTo("GOOG"))
						.withTag(TAG_REPORTING_OBLIGATION, equalTo("Y"))
						.withTag(TAG_TRADE_REPORT_TRANS_TYPE, equalTo("0"))
		).willReturn(respondWith(ack)));

		Message tcr = newTradeReportWithFinraTags("RPT-CT-002", "GOOG", 250, 2800.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Message response = app.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(response, "Should match on combination of standard + custom tags");
	}

	@Test
	void withTagName_works_after_registering_finra_aliases() throws Exception {
		// Gap #2 resolved: Register FINRA custom tag aliases so withTagName() works
		TagResolver.registerAlias("ReportingObligation", TAG_REPORTING_OBLIGATION);
		TagResolver.registerAliases(Map.of(
				"ControlDate", TAG_CONTROL_DATE,
				"ControlNumber", TAG_CONTROL_NUMBER,
				"MessageEventSource", TAG_MESSAGE_EVENT_SOURCE
		));

		int port = 9964;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		Message tcr = newTradeReportWithFinraTags("RPT-CT-003", "NFLX", 100, 600.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Thread.sleep(2000);

		// withTagName now resolves FINRA custom tag names after registerAlias()
		server.verify(exactly(1), within(Duration.ofSeconds(5)),
				tradeCaptureReport().withTagName("ReportingObligation", equalTo("Y")));
	}

	@Test
	void verify_custom_tags_in_response_message() throws Exception {
		int port = 9965;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Build response with FINRA custom tags (ControlDate, ControlNumber, MessageEventSource)
		Message ack = orfAcknowledgement("RPT-CT-004", "TRD-CT-004", EVENT_SOURCE_OREN, "AMZN");
		server.stubFor(when(
				appMessageRequestedFor().messageType("AE")
		).willReturn(respondWith(ack)));

		Message tcr = newTradeReportWithFinraTags("RPT-CT-004", "AMZN", 50, 3400.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Message response = app.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(response);

		// Verify FINRA-specific response fields
		assertEquals("20260217", response.getString(TAG_CONTROL_DATE));
		assertEquals("00001", response.getString(TAG_CONTROL_NUMBER));
		assertEquals(EVENT_SOURCE_OREN, response.getString(TAG_MESSAGE_EVENT_SOURCE));
	}

	@Test
	void value_matchers_work_with_custom_tags() throws Exception {
		int port = 9966;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		Message tcr = newTradeReportWithFinraTags("RPT-CT-005", "TSLA", 100, 250.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Thread.sleep(2000);

		// Matchers work with custom tags: in(), matching(), containing()
		server.verify(exactly(1), within(Duration.ofSeconds(5)),
				tradeCaptureReport().withTag(TAG_REPORTING_OBLIGATION, in("Y", "N")));

		server.verify(exactly(1), within(Duration.ofSeconds(5)),
				tradeCaptureReport().withTag(TAG_TRADE_REPORT_ID, matching("RPT-CT-.*")));

		server.verify(exactly(1), within(Duration.ofSeconds(5)),
				tradeCaptureReport().withTag(TAG_TRADE_REPORT_ID, containing("CT-005")));
	}
}
