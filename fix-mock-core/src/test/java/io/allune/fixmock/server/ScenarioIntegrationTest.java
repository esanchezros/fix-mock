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
package io.allune.fixmock.server;

import io.allune.fixmock.FixMock;
import io.allune.fixmock.FixMockServerOptions;
import io.allune.fixmock.stubbing.StubMapping;
import org.junit.jupiter.api.AfterEach;
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

import static io.allune.fixmock.FixMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioIntegrationTest {

	private SimpleFixMockServer server;
	private Initiator initiator;

	@AfterEach
	void tearDown() throws Exception {
		if (initiator != null) initiator.stop();
		if (server != null) server.stop();
	}

	@Test
	void shouldHandleStatefulTransitions() throws Exception {
		// Given: A scenario for Order lifecycle: Started -> Pending -> Filled
		StubMapping pendingStub = stubFor(newOrderSingle())
				.inScenario("OrderLifecycle")
				.whenScenarioStateIs("Started")
				.willSetStateTo("Pending")
				.willReturn(new ExecutionReport(
						new OrderID("ORD1"),
						new ExecID("EXEC1"),
						new ExecType(ExecType.PENDING_NEW),
						new OrdStatus(OrdStatus.PENDING_NEW),
						new Side(Side.BUY),
						new LeavesQty(100),
						new CumQty(0),
						new AvgPx(0)
				));

		StubMapping filledStub = stubFor(newOrderSingle())
				.inScenario("OrderLifecycle")
				.whenScenarioStateIs("Pending")
				.willSetStateTo("Filled")
				.willReturn(new ExecutionReport(
						new OrderID("ORD1"),
						new ExecID("EXEC2"),
						new ExecType(ExecType.FILL),
						new OrdStatus(OrdStatus.FILLED),
						new Side(Side.BUY),
						new LeavesQty(0),
						new CumQty(100),
						new AvgPx(150.0)
				));

		int port = 9916;
		FixMockServerOptions options = FixMockServerOptions.builder()
				.beginString("FIX.4.4")
				.senderCompId("MOCK")
				.targetCompId("CLIENT")
				.acceptorPort(port)
				.stubMapping(pendingStub)
				.stubMapping(filledStub)
				.recordMode(FixMockServerOptions.RecordMode.PLAYBACK)
				.build();

		server = new SimpleFixMockServer(options);
		server.start();
		assertTrue(server.isRunning());

		// When: Client sends two orders
		CapturingApp app = new CapturingApp("AAPL", 2);
		initiator = startInitiator(app, "CLIENT", "MOCK", port);

		List<Message> ers = app.awaitAppMessages(5, TimeUnit.SECONDS);

		// Then: First ER is PENDING, second is FILL
		assertNotNull(ers);
		assertEquals(2, ers.size(), "Should receive two ER responses");
		assertEquals(ExecType.PENDING_NEW, ers.get(0).getChar(ExecType.FIELD));
		assertEquals(ExecType.FILL, ers.get(1).getChar(ExecType.FIELD));
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

	static class CapturingApp extends MessageCracker implements Application {
		private final String symbol;
		private final int count;
		private final CountDownLatch appLatch;
		private final List<Message> received = new ArrayList<>();

		CapturingApp(String symbol, int count) {
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
					Thread.sleep(200); // Give some time between orders
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
