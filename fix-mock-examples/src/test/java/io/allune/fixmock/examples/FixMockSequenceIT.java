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
import io.allune.fixmock.server.SimpleFixMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.NewOrderSingle;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.allune.fixmock.FixMock.*;
import static org.junit.jupiter.api.Assertions.*;

/** Sequence responses: first response X, then Y for subsequent matching requests. */
public class FixMockSequenceIT {

	private FixMockServer server;
	private Initiator initiator;

	@BeforeEach void setUp() { server = null; initiator = null; }
	@AfterEach void tearDown() throws Exception { if (initiator != null) initiator.stop(); if (server != null) server.stop(); }

	@Test
	void sequence_returns_new_then_partial_for_two_orders() throws Exception {
		int port = 9912;
		server = new SimpleFixMockServer("FIX.4.4", "BROKER", "CLIENT", port);
		server.start();
		assertTrue(server.isRunning());

		// Build two ER responses: NEW, then PARTIAL
		ExecutionReport erNew = new ExecutionReport(
				new OrderID("OID1"), new ExecID("E1"),
				new ExecType(ExecType.NEW), new OrdStatus(OrdStatus.NEW),
				new Side(Side.BUY), new LeavesQty(100), new CumQty(0), new AvgPx(0)
		);
		ExecutionReport erPartial = new ExecutionReport(
				new OrderID("OID1"), new ExecID("E2"),
				new ExecType(ExecType.PARTIAL_FILL), new OrdStatus(OrdStatus.PARTIALLY_FILLED),
				new Side(Side.BUY), new LeavesQty(50), new CumQty(50), new AvgPx(150)
		);

		server.stubFor(
				when(appMessageRequestedFor().messageType("D"))
						.willReturn(FixMock.sequence(
								FixMock.respondWith(erNew),
								FixMock.respondWith(erPartial)
						))
		);

		// Initiator app that sends two NOS and captures two ERs
		CapturingTwoERsApp app = new CapturingTwoERsApp("AAPL", 2);
		initiator = startInitiator(app, "CLIENT", "BROKER", port);

		List<Message> ers = app.awaitAppMessages(5, TimeUnit.SECONDS);
		assertNotNull(ers);
		assertEquals(2, ers.size(), "Should receive two ER responses");
		assertEquals("8", ers.get(0).getHeader().getString(MsgType.FIELD));
		assertEquals(ExecType.NEW, ers.get(0).getChar(ExecType.FIELD));
		assertEquals(ExecType.PARTIAL_FILL, ers.get(1).getChar(ExecType.FIELD));
	}

	private static Initiator startInitiator(Application app, String sender, String target, int port) throws ConfigError, InterruptedException {
		SessionSettings settings = initiatorSettings(sender, target, port);
		MessageStoreFactory storeFactory = new MemoryStoreFactory();
		LogFactory logFactory = new ScreenLogFactory(true, true, true);
		MessageFactory messageFactory = new DefaultMessageFactory();
		Initiator initiator = new SocketInitiator(app, storeFactory, settings, logFactory, messageFactory);
		initiator.start();
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

	static class CapturingTwoERsApp extends MessageCracker implements Application {
		private final String symbol;
		private final int expected;
		private final CountDownLatch appLatch;
		private final List<Message> received = new ArrayList<>();

		CapturingTwoERsApp(String symbol, int expected) {
			this.symbol = symbol;
			this.expected = expected;
			this.appLatch = new CountDownLatch(expected);
		}

		List<Message> awaitAppMessages(long time, TimeUnit unit) throws InterruptedException {
			if (!appLatch.await(time, unit)) return received;
			return received;
		}

		@Override public void onCreate(SessionID sessionId) {}
		@Override public void onLogon(SessionID sessionId) {
			try {
				// Send two NOS with different ClOrdIDs
				for (int i = 0; i < expected; i++) {
					NewOrderSingle nos = new NewOrderSingle(
							new ClOrdID(UUID.randomUUID().toString()),
							new Side(Side.BUY),
							new TransactTime(LocalDateTime.now()),
							new OrdType(OrdType.MARKET)
					);
					nos.set(new Symbol(symbol));
					Session.sendToTarget(nos, sessionId);
				}
			} catch (Exception ignored) {}
		}
		@Override public void onLogout(SessionID sessionId) {}
		@Override public void toAdmin(Message message, SessionID sessionId) {}
		@Override public void fromAdmin(Message message, SessionID sessionId) {}
		@Override public void toApp(Message message, SessionID sessionId) {}
		@Override public void fromApp(Message message, SessionID sessionId) { received.add(message); appLatch.countDown(); }
	}
}
