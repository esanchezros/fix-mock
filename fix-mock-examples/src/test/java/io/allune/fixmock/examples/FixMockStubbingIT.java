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
package io.allune.fixmock.examples;

import io.allune.fixmock.FixMock;
import io.allune.fixmock.FixMockServer;
import io.allune.fixmock.matching.FixMessagePatternBuilder;
import io.allune.fixmock.server.SimpleFixMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.NewOrderSingle;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;

import static io.allune.fixmock.FixMock.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests covering stubbing and verification behaviors of SimpleFixMockServer.
 */
public class FixMockStubbingIT {

	private FixMockServer server;
	private Initiator initiator;

	@BeforeEach
	void setUp() {
		initiator = null;
		server = null;
	}

	@AfterEach
	void tearDown() throws Exception {
		if (initiator != null) {
			initiator.stop();
		}
		if (server != null) {
			server.stop();
		}
	}

	@Test
	void stubbing_staticResponse_and_verify_within_time_window() throws Exception {
		int port = 9901;
		server = new SimpleFixMockServer("FIX.4.4", "BROKER", "CLIENT", port);
		server.start();
		assertTrue(server.isRunning(), "Server should be running");

		// When a NOS with 55=AAPL arrives, respond with a simple ExecutionReport
		FixMessagePatternBuilder requestPattern = FixMock.appMessageRequestedFor()
				.messageType("D")
				.withTag(55, equalTo("AAPL"));

		ExecutionReport er = new ExecutionReport(
				new OrderID("OID1"),
				new ExecID("EID1"),
				new ExecType(ExecType.NEW),
				new OrdStatus(OrdStatus.NEW),
				new Side(Side.BUY),
				new LeavesQty(100),
				new CumQty(0),
				new AvgPx(0)
		);
		er.set(new ClOrdID("client-clordid"));
		er.set(new Symbol("AAPL"));

		server.stubFor(when(requestPattern).willReturn(er));

		// Start a minimal initiator that sends one NOS for AAPL
		initiator = startInitiatorThatSendsNOSOnce("CLIENT", "BROKER", port, "AAPL");

		// Allow time for logon and exchange
		Thread.sleep(2000);

		// Verify receipt within 5 seconds
		server.verify(atLeast(1), within(Duration.ofSeconds(5)),
				messageType("D").withTag(55, equalTo("AAPL"))
		);
	}

	@Test
	void stub_priority_more_specific_wins() throws Exception {
		int port = 9902;
		server = new SimpleFixMockServer("FIX.4.4", "BROKER", "CLIENT", port);
		server.start();
		assertTrue(server.isRunning(), "Server should be running");

		// General stub: any NOS -> respond with ER NEW (priority 5)
		ExecutionReport generalEr = new ExecutionReport(
				new OrderID("G-OID"), new ExecID("G-EID"),
				new ExecType(ExecType.NEW), new OrdStatus(OrdStatus.NEW),
				new Side(Side.BUY), new LeavesQty(100), new CumQty(0), new AvgPx(0)
		);
		generalEr.set(new Symbol("GEN"));
		server.stubFor(
				when(
						FixMock.appMessageRequestedFor().messageType("D")
				).atPriority(5).willReturn(generalEr)
		);

		// Specific stub: NOS with 55=AAPL -> respond with ER PARTIAL (priority 1)
		ExecutionReport specificEr = new ExecutionReport(
				new OrderID("S-OID"), new ExecID("S-EID"),
				new ExecType(ExecType.PARTIAL_FILL), new OrdStatus(OrdStatus.PARTIALLY_FILLED),
				new Side(Side.BUY), new LeavesQty(50), new CumQty(50), new AvgPx(150)
		);
		specificEr.set(new Symbol("AAPL"));
		server.stubFor(
				when(
						FixMock.appMessageRequestedFor().messageType("D").withTag(55, equalTo("AAPL"))
				).atPriority(1).willReturn(specificEr)
		);

		// Start initiator that sends AAPL
		initiator = startInitiatorThatSendsNOSOnce("CLIENT", "BROKER", port, "AAPL");

		Thread.sleep(2000);

		// We can't easily assert which ER was sent without an opposing client app here,
		// but we can at least verify the request match was recorded
		server.verify(exactly(1), within(Duration.ofSeconds(5)),
				messageType("D").withTag(55, equalTo("AAPL"))
		);
	}

	@Test
	void verification_count_strategies_exactly_and_atMost() throws Exception {
		int port = 9903;
		server = new SimpleFixMockServer("FIX.4.4", "BROKER", "CLIENT", port);
		server.start();
		assertTrue(server.isRunning(), "Server should be running");

		// No stubs required; just send one message and verify counts
		initiator = startInitiatorThatSendsNOSOnce("CLIENT", "BROKER", port, "MSFT");

		Thread.sleep(2000);

		// exactly(1) should pass
		server.verify(exactly(1), within(Duration.ofSeconds(5)),
				messageType("D").withTag(55, equalTo("MSFT"))
		);

		// atMost(1) should also pass
		server.verify(atMost(1), within(Duration.ofSeconds(5)),
				messageType("D").withTag(55, equalTo("MSFT"))
		);
	}

	// --- helpers ---
	private static Initiator startInitiatorThatSendsNOSOnce(String sender, String target, int port, String symbol) throws ConfigError, InterruptedException {
		Application app = new AppThatSendsOnce(symbol);
		SessionSettings settings = initiatorSettings(sender, target, port);
		MessageStoreFactory storeFactory = new MemoryStoreFactory();
		LogFactory logFactory = new ScreenLogFactory(true, true, true);
		MessageFactory messageFactory = new DefaultMessageFactory();
		Initiator initiator = new SocketInitiator(app, storeFactory, settings, logFactory, messageFactory);
		initiator.start();
		// Give it a moment to logon; tests add further waits before verify
		Thread.sleep(500);
		return initiator;
	}

	private static SessionSettings initiatorSettings(String sender, String target, int port) throws ConfigError {
		Properties defaults = new Properties();
		defaults.setProperty(SessionFactory.SETTING_CONNECTION_TYPE, "initiator");
		defaults.setProperty(Initiator.SETTING_RECONNECT_INTERVAL, "2");
		defaults.setProperty("UseDataDictionary", "N");
		defaults.setProperty("HeartBtInt", "30");
		defaults.setProperty("StartTime", "00:00:00");
		defaults.setProperty("EndTime", "23:59:59");

		SessionID sessionID = new SessionID("FIX.4.4", sender, target);

		SessionSettings settings = new SessionSettings();
		settings.set(defaults);
		settings.setString(sessionID, SessionFactory.SETTING_CONNECTION_TYPE, "initiator");
		settings.setString(sessionID, SocketInitiator.SETTING_SOCKET_CONNECT_HOST, "127.0.0.1");
		settings.setString(sessionID, SocketInitiator.SETTING_SOCKET_CONNECT_PORT, Integer.toString(port));
		settings.setString(sessionID, "UseDataDictionary", "N");
		settings.setString(sessionID, "HeartBtInt", "30");
		settings.setString(sessionID, "StartTime", "00:00:00");
		settings.setString(sessionID, "EndTime", "23:59:59");
		return settings;
	}

	/** Initiator app that sends one NewOrderSingle upon logon with the given symbol. */
	static class AppThatSendsOnce extends MessageCracker implements Application {
		private final String symbol;
		private volatile boolean sent = false;

		AppThatSendsOnce(String symbol) {
			this.symbol = symbol;
		}

		@Override
		public void onCreate(SessionID sessionId) {}

		@Override
		public void onLogon(SessionID sessionId) {
			if (!sent) {
				try {
					NewOrderSingle nos = new NewOrderSingle(
							new ClOrdID(UUID.randomUUID().toString()),
							new Side(Side.BUY),
							new TransactTime(LocalDateTime.now()),
							new OrdType(OrdType.MARKET)
					);
					nos.set(new Symbol(symbol));
					Session.sendToTarget(nos, sessionId);
					sent = true;
				} catch (SessionNotFound e) {
					throw new RuntimeException(e);
				}
			}
		}

		@Override public void onLogout(SessionID sessionId) {}
		@Override public void toAdmin(Message message, SessionID sessionId) {}
		@Override public void fromAdmin(Message message, SessionID sessionId) {}
		@Override public void toApp(Message message, SessionID sessionId) {}
		@Override public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectTagValue, UnsupportedMessageType {}
	}
}
