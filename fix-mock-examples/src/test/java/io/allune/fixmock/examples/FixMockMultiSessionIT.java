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
import io.allune.fixmock.FixMockServerOptions;
import io.allune.fixmock.SessionConfig;
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
 * Integration test for SimpleFixMockServer with multiple sessions.
 */
public class FixMockMultiSessionIT {

	private FixMockServer server;
	private Initiator initiator1;
	private Initiator initiator2;
	private CapturingApp app1;
	private CapturingApp app2;

	@BeforeEach
	void setUp() {
		server = null;
		initiator1 = null;
		initiator2 = null;
	}

	@AfterEach
	void tearDown() throws Exception {
		if (initiator1 != null) initiator1.stop();
		if (initiator2 != null) initiator2.stop();
		if (server != null) server.stop();
	}

	@Test
	void server_handles_two_independent_sessions() throws Exception {
		int port1 = 9921;
		int port2 = 9922;

		// Configure mock with TWO sessions
		FixMockServerOptions options = FixMockServerOptions.builder()
				.addSession(SessionConfig.builder()
						.beginString("FIX.4.4")
						.senderCompId("BROKER1")
						.targetCompId("CLIENT1")
						.port(port1)
						.role(FixMockServerOptions.Role.ACCEPTOR)
						.build())
				.addSession(SessionConfig.builder()
						.beginString("FIX.4.4")
						.senderCompId("BROKER2")
						.targetCompId("CLIENT2")
						.port(port2)
						.role(FixMockServerOptions.Role.ACCEPTOR)
						.build())
				.build();

		server = new SimpleFixMockServer(options);
		server.start();
		assertTrue(server.isRunning());

		// Stub session 1: NOS -> ER(NEW)
		ExecutionReport er1 = new ExecutionReport(
				new OrderID("OID1"), new ExecID("E1"),
				new ExecType(ExecType.NEW), new OrdStatus(OrdStatus.NEW),
				new Side(Side.BUY), new LeavesQty(100), new CumQty(0), new AvgPx(0)
		);
		server.stubFor(when(appMessageRequestedFor()
						.senderCompId("CLIENT1")
						.messageType("D"))
				.willReturn(respondWith(er1)));

		// Stub session 2: NOS -> ER(REJECT)
		ExecutionReport er2 = new ExecutionReport(
				new OrderID("OID2"), new ExecID("E2"),
				new ExecType(ExecType.REJECTED), new OrdStatus(OrdStatus.REJECTED),
				new Side(Side.SELL), new LeavesQty(0), new CumQty(0), new AvgPx(0)
		);
		server.stubFor(when(appMessageRequestedFor()
						.senderCompId("CLIENT2")
						.messageType("D"))
				.willReturn(respondWith(er2)));

		// Start two initiators
		app1 = new CapturingApp("CLIENT1", "BROKER1");
		initiator1 = startInitiator(app1, "CLIENT1", "BROKER1", port1);
		
		app2 = new CapturingApp("CLIENT2", "BROKER2");
		initiator2 = startInitiator(app2, "CLIENT2", "BROKER2", port2);

		// Wait for both sessions to log on
		assertTrue(((SimpleFixMockServer) server).awaitLogon(Duration.ofSeconds(5)), "Sessions should log on");

		// Send from app1
		sendNOS(initiator1, "CLIENT1", "BROKER1", "AAPL");
		Message resp1 = app1.awaitAppMessage(5, TimeUnit.SECONDS);
		assertNotNull(resp1);
		assertEquals(ExecType.NEW, resp1.getChar(ExecType.FIELD));

		// Send from app2
		sendNOS(initiator2, "CLIENT2", "BROKER2", "MSFT");
		Message resp2 = app2.awaitAppMessage(5, TimeUnit.SECONDS);
		assertNotNull(resp2);
		assertEquals(ExecType.REJECTED, resp2.getChar(ExecType.FIELD));

		// Verify independent journals
		server.verify(exactly(1), appMessageRequestedFor().senderCompId("CLIENT1").messageType("D"));
		server.verify(exactly(1), appMessageRequestedFor().senderCompId("CLIENT2").messageType("D"));
		
		// Use the new session-scoped helper
		server.session("FIX.4.4:BROKER1->CLIENT1").verify(exactly(1), appMessageRequestedFor().messageType("D"));
		server.session("FIX.4.4:BROKER2->CLIENT2").verify(exactly(1), appMessageRequestedFor().messageType("D"));

		// Combined verification (if no session filter applied)
		server.verify(exactly(2), appMessageRequestedFor().messageType("D"));
	}

	private void sendNOS(Initiator initiator, String sender, String target, String symbol) throws SessionNotFound {
		SessionID sid = new SessionID("FIX.4.4", sender, target);
		NewOrderSingle nos = new NewOrderSingle(
				new ClOrdID(UUID.randomUUID().toString()),
				new Side(Side.BUY),
				new TransactTime(LocalDateTime.now()),
				new OrdType(OrdType.MARKET)
		);
		nos.set(new Symbol(symbol));
		Session.sendToTarget(nos, sid);
	}

	private static Initiator startInitiator(Application app, String sender, String target, int port) throws ConfigError {
		SessionSettings settings = new SessionSettings();
		Properties defaults = new Properties();
		defaults.setProperty(SessionFactory.SETTING_CONNECTION_TYPE, "initiator");
		defaults.setProperty(Initiator.SETTING_RECONNECT_INTERVAL, "2");
		defaults.setProperty("UseDataDictionary", "N");
		defaults.setProperty("HeartBtInt", "30");
		defaults.setProperty("StartTime", "00:00:00");
		defaults.setProperty("EndTime", "23:59:59");
		settings.set(defaults);

		SessionID sessionID = new SessionID("FIX.4.4", sender, target);
		settings.setString(sessionID, SessionFactory.SETTING_CONNECTION_TYPE, "initiator");
		settings.setString(sessionID, SocketInitiator.SETTING_SOCKET_CONNECT_HOST, "127.0.0.1");
		settings.setString(sessionID, SocketInitiator.SETTING_SOCKET_CONNECT_PORT, Integer.toString(port));

		MessageStoreFactory storeFactory = new MemoryStoreFactory();
		LogFactory logFactory = new ScreenLogFactory(true, true, true);
		MessageFactory messageFactory = new DefaultMessageFactory();
		Initiator initiator = new SocketInitiator(app, storeFactory, settings, logFactory, messageFactory);
		initiator.start();
		return initiator;
	}

	static class CapturingApp extends MessageCracker implements Application {
		private final CountDownLatch latch = new CountDownLatch(1);
		private volatile Message lastMsg;
		private final String sender;
		private final String target;

		CapturingApp(String sender, String target) {
			this.sender = sender;
			this.target = target;
		}

		Message awaitAppMessage(long timeout, TimeUnit unit) throws InterruptedException {
			latch.await(timeout, unit);
			return lastMsg;
		}

		@Override public void onCreate(SessionID sessionId) {}
		@Override public void onLogon(SessionID sessionId) {}
		@Override public void onLogout(SessionID sessionId) {}
		@Override public void toAdmin(Message message, SessionID sessionId) {}
		@Override public void fromAdmin(Message message, SessionID sessionId) {}
		@Override public void toApp(Message message, SessionID sessionId) {}
		@Override public void fromApp(Message message, SessionID sessionId) {
			lastMsg = message;
			latch.countDown();
		}
	}
}
