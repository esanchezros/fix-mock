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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.allune.fixmock.FixMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for response strategies: templating (copyFromRequestTags) and delays.
 */
public class FixMockResponseStrategiesIT {

	private FixMockServer server;
	private Initiator initiator;

	@BeforeEach
	void setUp() {
		initiator = null;
		server = null;
	}

	@AfterEach
	void tearDown() throws Exception {
		if (initiator != null) initiator.stop();
		if (server != null) server.stop();
	}

	@Test
	void templated_response_copies_ClOrdID_and_Symbol() throws Exception {
		int port = 9906;
		server = new SimpleFixMockServer("FIX.4.4", "BROKER", "CLIENT", port);
		server.start();
		assertTrue(server.isRunning());

		// Build templated ER supplier (fresh ER per request)
		FixMock.ResponseTemplateBuilder response = FixMock.respondWithTemplate(() -> new ExecutionReport(
				new OrderID("OID-TPL"), new ExecID("EID-TPL"),
				new ExecType(ExecType.NEW), new OrdStatus(OrdStatus.NEW),
				new Side(Side.BUY), new LeavesQty(100), new CumQty(0), new AvgPx(0)
		)).copyFromRequestTags(11, 55); // copy ClOrdID(11) and Symbol(55)

		server.stubFor(when(
						FixMock.appMessageRequestedFor().messageType("D")
				).willReturn(response)
		);

		// Initiator that captures the ER and exposes it to the test
		CapturingApp app = new CapturingApp("MSFT");
		initiator = startInitiator(app, "CLIENT", "BROKER", port);

		Message er = app.awaitAppMessage(5, TimeUnit.SECONDS);
		assertNotNull(er, "Expected ExecutionReport");
		assertEquals("8", er.getHeader().getString(MsgType.FIELD));
		assertTrue(er.isSetField(ClOrdID.FIELD));
		assertEquals(app.getLastSentClOrdId(), er.getString(ClOrdID.FIELD));
		assertEquals("MSFT", er.getString(Symbol.FIELD));
	}

	@Test
	void delayed_response_waits_at_least_configured_time() throws Exception {
		int port = 9907;
		server = new SimpleFixMockServer("FIX.4.4", "BROKER", "CLIENT", port);
		server.start();
		assertTrue(server.isRunning());

		long configuredDelayMs = 400;

		FixMock.ResponseTemplateBuilder response = FixMock.respondWithTemplate(() -> new ExecutionReport(
				new OrderID("OID-DLY"), new ExecID("EID-DLY"),
				new ExecType(ExecType.NEW), new OrdStatus(OrdStatus.NEW),
				new Side(Side.BUY), new LeavesQty(100), new CumQty(0), new AvgPx(0)
		)).delay(Duration.ofMillis(configuredDelayMs));

		server.stubFor(when(
				FixMock.appMessageRequestedFor().messageType("D")
		).willReturn(response));

		CapturingApp app = new CapturingApp("AAPL");
		initiator = startInitiator(app, "CLIENT", "BROKER", port);

		long start = System.currentTimeMillis();
		Message er = app.awaitAppMessage(5, TimeUnit.SECONDS);
		long elapsed = System.currentTimeMillis() - start;

		assertNotNull(er, "Expected ExecutionReport");
		assertTrue(elapsed >= configuredDelayMs - 50, "Response should be delayed (~" + configuredDelayMs + "ms), elapsed=" + elapsed);
	}

	// --- Initiator wiring and helpers ---
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

	/** Application that sends one NOS with a provided symbol on logon and captures inbound app messages. */
	static class CapturingApp extends MessageCracker implements Application {
		private final String symbolToSend;
		private final CountDownLatch appLatch = new CountDownLatch(1);
		private volatile Message lastAppMessage;
		private volatile String lastSentClOrdId;

		CapturingApp(String symbol) { this.symbolToSend = symbol; }

		String getLastSentClOrdId() { return lastSentClOrdId; }

		Message awaitAppMessage(long time, TimeUnit unit) throws InterruptedException {
			if (!appLatch.await(time, unit)) return null;
			return lastAppMessage;
		}

		@Override public void onCreate(SessionID sessionId) {}
		@Override public void onLogon(SessionID sessionId) {
			try {
				lastSentClOrdId = UUID.randomUUID().toString();
				NewOrderSingle nos = new NewOrderSingle(
						new ClOrdID(lastSentClOrdId),
						new Side(Side.BUY),
						new TransactTime(LocalDateTime.now()),
						new OrdType(OrdType.MARKET)
				);
				nos.set(new Symbol(symbolToSend));
				Session.sendToTarget(nos, sessionId);
			} catch (Exception ignored) {}
		}
		@Override public void onLogout(SessionID sessionId) {}
		@Override public void toAdmin(Message message, SessionID sessionId) {}
		@Override public void fromAdmin(Message message, SessionID sessionId) {}
		@Override public void toApp(Message message, SessionID sessionId) {}
		@Override public void fromApp(Message message, SessionID sessionId) { lastAppMessage = message; appLatch.countDown(); }
	}
}
