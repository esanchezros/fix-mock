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

import static io.allune.fixmock.FixMock.*;
import io.allune.fixmock.server.SimpleFixMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.*;
import quickfix.field.*;
import quickfix.field.MsgType;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.NewOrderSingle;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Realistic example: Order lifecycle (NOS → ER NEW → ER PARTIAL → ER FILL)
 *
 * Note: Current mock model returns one response per matching request. To model a
 * lifecycle progression, we send three NOS messages and stub a response sequence
 * so the three ERs arrive as NEW, PARTIAL, then FILL.
 */
public class FixMockOrderLifecycleIT {

	private FixMockServer server;
	private Initiator initiator;

	@BeforeEach void setUp() { server = null; initiator = null; }
	@AfterEach void tearDown() throws Exception { if (initiator != null) initiator.stop(); if (server != null) server.stop(); }

	@Test
	void order_lifecycle_progresses_new_partial_fill_across_three_orders() throws Exception {
		int port = 9915;
		server = new SimpleFixMockServer("FIX.4.4", "BROKER", "CLIENT", port);
		server.start();
		assertTrue(server.isRunning());

		// Prepare three ER responses: NEW, PARTIAL, FILL
		ExecutionReport erNew = new ExecutionReport(
				new OrderID("OID-LC-1"), new ExecID("LC-E1"),
				new ExecType(ExecType.NEW), new OrdStatus(OrdStatus.NEW),
				new Side(Side.BUY), new LeavesQty(100), new CumQty(0), new AvgPx(0)
		);
		ExecutionReport erPartial = new ExecutionReport(
				new OrderID("OID-LC-1"), new ExecID("LC-E2"),
				new ExecType(ExecType.PARTIAL_FILL), new OrdStatus(OrdStatus.PARTIALLY_FILLED),
				new Side(Side.BUY), new LeavesQty(50), new CumQty(50), new AvgPx(150)
		);
		ExecutionReport erFill = new ExecutionReport(
				new OrderID("OID-LC-1"), new ExecID("LC-E3"),
				new ExecType(ExecType.FILL), new OrdStatus(OrdStatus.FILLED),
				new Side(Side.BUY), new LeavesQty(0), new CumQty(100), new AvgPx(155)
		);

		server.stubFor(
				when(newOrderSingle())
						.willReturn(FixMock.sequence(
								FixMock.respondWith(erNew),
								FixMock.respondWith(erPartial),
								FixMock.respondWith(erFill)
						))
		);

		// App will send 3 NOS; capture 3 ERs
		ThreeNosCapturingApp app = new ThreeNosCapturingApp("AAPL", 3);
		initiator = startInitiator(app, "CLIENT", "BROKER", port);

		List<Message> ers = app.awaitAppMessages(5, TimeUnit.SECONDS);
		assertNotNull(ers);
		assertEquals(3, ers.size(), "Should receive three ER responses");
		assertEquals(ExecType.NEW, ers.get(0).getChar(ExecType.FIELD));
		assertEquals(ExecType.PARTIAL_FILL, ers.get(1).getChar(ExecType.FIELD));
		assertEquals(ExecType.FILL, ers.get(2).getChar(ExecType.FIELD));
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

	static class ThreeNosCapturingApp extends MessageCracker implements Application {
		private final String symbol;
		private final int count;
		private final CountDownLatch appLatch;
		private final List<Message> received = new ArrayList<>();

		ThreeNosCapturingApp(String symbol, int count) {
			this.symbol = symbol;
			this.count = count;
			this.appLatch = new CountDownLatch(count);
		}

		List<Message> awaitAppMessages(long time, TimeUnit unit) throws InterruptedException {
			appLatch.await(time, unit);
			return received;
		}

		@Override public void onCreate(SessionID sessionId) {}
		@Override public void onLogon(SessionID sessionId) {
			try {
				for (int i = 0; i < count; i++) {
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
