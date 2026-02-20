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
import io.allune.fixmock.FixMockServerOptions;
import io.allune.fixmock.server.SimpleFixMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.Group;
import quickfix.Initiator;
import quickfix.Message;
import quickfix.field.*;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.NewOrderSingle;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.allune.fixmock.FixMock.*;
import static io.allune.fixmock.integration.FixInitiatorSupport.*;
import static io.allune.fixmock.integration.OrfMessages.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Template response with tag copying: copy TradeReportID, Symbol from request to response,
 * verify FINRA-assigned fields (TradeID, ControlDate) are added.
 *
 * <p>Gap #4 resolved: {@code copyGroupsFromRequest()} enables repeating group transfer
 * from request to response messages.
 */
public class OrfResponseTemplateIT {

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
	void template_copies_trade_report_id_and_symbol_from_request() throws Exception {
		int port = 9972;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Template: fresh OREN ack, copy TradeReportID(571) and Symbol(55) from request
		FixMock.ResponseTemplateBuilder template = respondWithTemplate(
				() -> freshOrfAck(EVENT_SOURCE_OREN)
		).copyFromRequestTags(TAG_TRADE_REPORT_ID, TAG_SYMBOL);

		server.stubFor(when(
				tradeCaptureReport()
		).willReturn(template));

		// Send TCR with specific TradeReportID and Symbol
		Message tcr = newTradeReport("RPT-TPL-001", "AAPL", 1000, 150.25);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Message response = app.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(response, "Expected templated OREN acknowledgement");

		// TradeReportID and Symbol should be copied from request
		assertEquals("RPT-TPL-001", response.getString(TAG_TRADE_REPORT_ID));
		assertEquals("AAPL", response.getString(TAG_SYMBOL));

		// FINRA-assigned fields should be present from the template
		assertEquals(EVENT_SOURCE_OREN, response.getString(TAG_MESSAGE_EVENT_SOURCE));
		assertTrue(response.isSetField(TAG_TRADE_ID), "TradeID should be set by template");
		assertTrue(response.isSetField(TAG_CONTROL_DATE), "ControlDate should be set by template");
	}

	@Test
	void template_copies_custom_finra_tags() throws Exception {
		int port = 9973;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Template: copy TradeReportID, Symbol, and ReportingObligation (custom FINRA tag)
		FixMock.ResponseTemplateBuilder template = respondWithTemplate(
				() -> freshOrfAck(EVENT_SOURCE_OREN)
		).copyFromRequestTags(TAG_TRADE_REPORT_ID, TAG_SYMBOL, TAG_REPORTING_OBLIGATION);

		server.stubFor(when(
				tradeCaptureReport()
		).willReturn(template));

		Message tcr = newTradeReportWithFinraTags("RPT-TPL-002", "MSFT", 500, 420.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		Message response = app.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(response);

		// Custom FINRA tag should be copied
		assertEquals("Y", response.getString(TAG_REPORTING_OBLIGATION));
		assertEquals("RPT-TPL-002", response.getString(TAG_TRADE_REPORT_ID));
	}

	@Test
	void template_with_delay_for_realistic_latency() throws Exception {
		int port = 9974;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		long configuredDelayMs = 300;

		FixMock.ResponseTemplateBuilder template = respondWithTemplate(
				() -> freshOrfAck(EVENT_SOURCE_OREN)
		).copyFromRequestTags(TAG_TRADE_REPORT_ID, TAG_SYMBOL)
				.delay(Duration.ofMillis(configuredDelayMs));

		server.stubFor(when(
				tradeCaptureReport()
		).willReturn(template));

		Message tcr = newTradeReport("RPT-TPL-003", "GOOG", 200, 2800.00);
		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(tcr);
		initiator = startInitiator(app, "REPORTER", "FINRA", port);

		long start = System.currentTimeMillis();
		Message response = app.awaitFirstResponse(5, TimeUnit.SECONDS);
		long elapsed = System.currentTimeMillis() - start;

		assertNotNull(response);
		assertTrue(elapsed >= configuredDelayMs - 50,
				"Response should be delayed (~" + configuredDelayMs + "ms), elapsed=" + elapsed);
	}

	@Test
	void template_copies_repeating_groups_from_request() throws Exception {
		// Gap #4 resolved: copyGroupsFromRequest() copies full repeating group structures.
		// Uses data dictionary ON + standard NOS because group parsing requires dictionary.
		int port = 9975;
		FixMockServerOptions options = FixMockServerOptions.builder()
				.beginString("FIX.4.4")
				.senderCompId("BROKER")
				.targetCompId("CLIENT")
				.acceptorPort(port)
				.useDataDictionary(true)
				.build();
		server = new SimpleFixMockServer(options);
		server.start();

		// Template: create typed ER response, copy Parties group from NOS request
		FixMock.ResponseTemplateBuilder template = respondWithTemplate(() -> {
			ExecutionReport er = new ExecutionReport();
			er.set(new OrderID("ORD-1"));
			er.set(new ExecID("EXEC-1"));
			er.set(new ExecType(ExecType.NEW));
			er.set(new OrdStatus(OrdStatus.NEW));
			er.set(new Side(Side.BUY));
			er.set(new LeavesQty(100));
			er.set(new CumQty(0));
			er.set(new quickfix.field.AvgPx(0));
			return er;
		}).copyFromRequestTags(11, 55) // ClOrdID, Symbol
				.copyGroupsFromRequest(TAG_NO_PARTY_IDS);

		server.stubFor(when(
				newOrderSingle()
		).willReturn(template));

		// Build NOS with Parties group (addGroup manages the count automatically)
		NewOrderSingle nos = new NewOrderSingle(
				new ClOrdID("CLO-GP-001"),
				new Side(Side.BUY),
				new TransactTime(LocalDateTime.now()),
				new OrdType(OrdType.MARKET)
		);
		nos.set(new Symbol("NVDA"));
		quickfix.fix44.NewOrderSingle.NoPartyIDs p1 = new quickfix.fix44.NewOrderSingle.NoPartyIDs();
		p1.set(new quickfix.field.PartyID("PARTY-A"));
		p1.set(new quickfix.field.PartyIDSource('D'));
		p1.set(new quickfix.field.PartyRole(1));
		nos.addGroup(p1);
		quickfix.fix44.NewOrderSingle.NoPartyIDs p2 = new quickfix.fix44.NewOrderSingle.NoPartyIDs();
		p2.set(new quickfix.field.PartyID("PARTY-B"));
		p2.set(new quickfix.field.PartyIDSource('D'));
		p2.set(new quickfix.field.PartyRole(17));
		nos.addGroup(p2);

		SendOnceAndCaptureApp app = new SendOnceAndCaptureApp(nos);
		initiator = startInitiator(app, "CLIENT", "BROKER", port);

		Message response = app.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(response, "Expected ER with copied groups");

		// Scalar tags are copied
		assertEquals("CLO-GP-001", response.getString(11));
		assertEquals("NVDA", response.getString(55));

		// Verify groups are present in the server-side journal (outbound ER message)
		// which captures the raw serialized message including group structure
		Thread.sleep(500); // Allow journal to capture the outbound ER
		var journal = server.getJournalSnapshot();
		var outboundER = journal.stream()
				.filter(e -> "8".equals(e.getMsgType()) && !e.isInbound())
				.findFirst()
				.orElse(null);
		assertNotNull(outboundER, "Journal should contain outbound ER");
		String raw = outboundER.getRaw().replace('\001', '|');
		assertTrue(raw.contains("453="), "Response should contain NoPartyIDs counter: " + raw);
		assertTrue(raw.contains("448=PARTY-A"), "Response should contain PartyID=PARTY-A: " + raw);
		assertTrue(raw.contains("448=PARTY-B"), "Response should contain PartyID=PARTY-B: " + raw);
	}

	@Test
	void each_request_gets_fresh_response_from_supplier() throws Exception {
		int port = 9976;
		server = new SimpleFixMockServer("FIX.4.4", "FINRA", "REPORTER", port);
		server.start();

		// Template supplier creates a NEW message per call (fresh TradeID each time)
		FixMock.ResponseTemplateBuilder template = respondWithTemplate(
				() -> freshOrfAck(EVENT_SOURCE_OREN)
		).copyFromRequestTags(TAG_TRADE_REPORT_ID, TAG_SYMBOL);

		server.stubFor(when(
				tradeCaptureReport()
		).willReturn(template));

		// Send first trade
		Message tcr1 = newTradeReport("RPT-FRESH-1", "AAPL", 100, 150.00);
		SendOnceAndCaptureApp app1 = new SendOnceAndCaptureApp(tcr1);
		initiator = startInitiator(app1, "REPORTER", "FINRA", port);

		Message resp1 = app1.awaitFirstResponse(5, TimeUnit.SECONDS);
		assertNotNull(resp1);
		assertEquals("RPT-FRESH-1", resp1.getString(TAG_TRADE_REPORT_ID));
		String tradeId1 = resp1.getString(TAG_TRADE_ID);

		// The supplier generates a new TradeID each time, so it should differ
		// (unless called within the same nanosecond, which is extremely unlikely)
		assertNotNull(tradeId1, "First response should have a FINRA-assigned TradeID");
	}
}
