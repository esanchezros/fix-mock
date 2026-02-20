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
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Realistic example: Order cancellation flow
 * NOS (D) → ER NEW → OCR (F) → ER CANCELED
 */
public class FixMockCancelFlowIT {

	private FixMockServer server;
	private Initiator initiator;

	@BeforeEach void setUp() { server = null; initiator = null; }
	@AfterEach void tearDown() throws Exception { if (initiator != null) initiator.stop(); if (server != null) server.stop(); }

	@Test
	void cancel_flow_returns_new_then_canceled() throws Exception {
		int port = 9916;
		server = new SimpleFixMockServer("FIX.4.4", "BROKER", "CLIENT", port);
		server.start();
		assertTrue(server.isRunning());

		// ER NEW response for NOS
		ExecutionReport erNew = new ExecutionReport(
				new OrderID("OID-CXL-1"), new ExecID("CX-E1"),
				new ExecType(ExecType.NEW), new OrdStatus(OrdStatus.NEW),
				new Side(Side.BUY), new LeavesQty(100), new CumQty(0), new AvgPx(0)
		);
		// ER CANCELED response for OCR
		ExecutionReport erCanceled = new ExecutionReport(
				new OrderID("OID-CXL-1"), new ExecID("CX-E2"),
				new ExecType(ExecType.CANCELED), new OrdStatus(OrdStatus.CANCELED),
				new Side(Side.BUY), new LeavesQty(0), new CumQty(0), new AvgPx(0)
		);

		// Stub responses based on message type
		server.stubFor(when(appMessageRequestedFor().messageType("D"))
				.willReturn(FixMock.respondWith(erNew)));
		server.stubFor(when(appMessageRequestedFor().messageType("F"))
				.willReturn(FixMock.respondWith(erCanceled)));

		// App: send NOS then OCR, capture two ERs
		CancelFlowApp app = new CancelFlowApp("NFLX");
		initiator = startInitiator(app, "CLIENT", "BROKER", port);

		List<Message> ers = app.awaitAppMessages(5, TimeUnit.SECONDS);
		assertNotNull(ers);
		assertEquals(2, ers.size());
		assertEquals(ExecType.NEW, ers.get(0).getChar(ExecType.FIELD));
		assertEquals(ExecType.CANCELED, ers.get(1).getChar(ExecType.FIELD));
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

	static class CancelFlowApp extends MessageCracker implements Application {
		private final String symbol;
		private final CountDownLatch appLatch = new CountDownLatch(2);
		private final List<Message> received = new ArrayList<>();
		private String origClOrdId;

		CancelFlowApp(String symbol) { this.symbol = symbol; }

		List<Message> awaitAppMessages(long time, TimeUnit unit) throws InterruptedException {
			appLatch.await(time, unit);
			return received;
		}

		@Override public void onCreate(SessionID sessionId) {}
		@Override public void onLogon(SessionID sessionId) {
			try {
				// Send initial NOS
				origClOrdId = UUID.randomUUID().toString();
				NewOrderSingle nos = new NewOrderSingle(
						new ClOrdID(origClOrdId),
						new Side(Side.BUY),
						new TransactTime(LocalDateTime.now()),
						new OrdType(OrdType.MARKET)
				);
				nos.set(new Symbol(symbol));
				Session.sendToTarget(nos, sessionId);

				// Immediately request cancel (OCR)
				OrderCancelRequest ocr = new OrderCancelRequest(
						new OrigClOrdID(origClOrdId),
						new ClOrdID(UUID.randomUUID().toString()),
						new Side(Side.BUY),
						new TransactTime(LocalDateTime.now())
				);
				ocr.set(new Symbol(symbol));
				Session.sendToTarget(ocr, sessionId);
			} catch (Exception ignored) {}
		}
		@Override public void onLogout(SessionID sessionId) {}
		@Override public void toAdmin(Message message, SessionID sessionId) {}
		@Override public void fromAdmin(Message message, SessionID sessionId) {}
		@Override public void toApp(Message message, SessionID sessionId) {}
		@Override public void fromApp(Message message, SessionID sessionId) { received.add(message); appLatch.countDown(); }
	}
}
