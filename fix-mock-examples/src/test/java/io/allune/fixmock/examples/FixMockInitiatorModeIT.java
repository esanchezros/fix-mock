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

import io.allune.fixmock.FixMockServer;
import io.allune.fixmock.FixMockServerOptions;
import io.allune.fixmock.server.SimpleFixMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.NewOrderSingle;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.allune.fixmock.FixMock.atLeast;
import static io.allune.fixmock.FixMock.messageType;
import static io.allune.fixmock.FixMock.within;
import static io.allune.quickfixj.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for SimpleFixMockServer in INITIATOR role.
 * Starts a minimal QuickFIX/J acceptor harness and verifies that:
 *  - the mock connects as initiator,
 *  - send(Message) delivers to the acceptor,
 *  - mock journals the outbound app message and can verify it.
 */
public class FixMockInitiatorModeIT {

	private FixMockServer server;
	private Acceptor acceptor;
	private HarnessApp harnessApp;

	@BeforeEach
	void setUp() {
		server = null;
		acceptor = null;
		harnessApp = null;
	}

	@AfterEach
	void tearDown() throws Exception {
		if (acceptor != null) acceptor.stop();
		if (server != null) server.stop();
	}

	@Test
	void mock_as_initiator_sends_NOS_and_journals_outbound() throws Exception {
		int port = 0;
		// Start a minimal acceptor harness
		harnessApp = new HarnessApp();
		acceptor = startAcceptorHarness("FIX.4.4", "BROKER", "CLIENT", port, harnessApp);
		port = boundPort((ThreadedSocketAcceptor) acceptor);

		// Configure mock as INITIATOR
		FixMockServerOptions options = FixMockServerOptions.builder()
				.role(FixMockServerOptions.Role.INITIATOR)
				.beginString("FIX.4.4")
				.senderCompId("CLIENT")
				.targetCompId("BROKER")
				.initiatorHost("127.0.0.1")
				.initiatorPort(port)
				.build();

		server = new SimpleFixMockServer(options);
		server.start();
		assertTrue(server.isRunning());

		// Wait for logon before sending
		assertTrue(((SimpleFixMockServer) server).awaitLogon(Duration.ofSeconds(3)), "Initiator should log on before sending");

		// Build and send a NOS
		String cid = UUID.randomUUID().toString();
		NewOrderSingle nos = new NewOrderSingle(
				new ClOrdID(cid),
				new Side(Side.BUY),
				new TransactTime(LocalDateTime.now()),
				new OrdType(OrdType.MARKET)
		);
		nos.set(new Symbol("AAPL"));
		server.send(nos);

		// Harness should receive it
		Message received = harnessApp.awaitAppMessage(5, TimeUnit.SECONDS);
		assertNotNull(received, "Harness should receive NOS");
		assertThat(received).isNewOrderSingle().hasFieldValue(ClOrdID.FIELD, cid).hasFieldValue(Symbol.FIELD, "AAPL");

		// And the mock should have journaled the outbound message
		server.verify(atLeast(1), within(Duration.ofSeconds(5)), messageType("D"));
	}

	private static int boundPort(ThreadedSocketAcceptor acceptor) {
		return acceptor.getEndpoints().stream()
				.map(org.apache.mina.core.service.IoAcceptor::getLocalAddress)
				.filter(java.net.InetSocketAddress.class::isInstance)
				.map(java.net.InetSocketAddress.class::cast)
				.mapToInt(java.net.InetSocketAddress::getPort)
				.findFirst()
				.orElseThrow();
	}

	// --- Acceptor harness
	private static Acceptor startAcceptorHarness(String beginString, String sender, String target, int port, Application app) throws ConfigError {
		SessionSettings settings = new SessionSettings();
		settings.setString(SessionFactory.SETTING_CONNECTION_TYPE, "acceptor");
		settings.setString(Acceptor.SETTING_SOCKET_ACCEPT_PORT, Integer.toString(port));
		settings.setString("UseDataDictionary", "N");
		settings.setString("HeartBtInt", "30");
		settings.setString("StartTime", "00:00:00");
		settings.setString("EndTime", "23:59:59");

		SessionID sid = new SessionID(beginString, sender, target);
		settings.setString(sid, SessionFactory.SETTING_CONNECTION_TYPE, "acceptor");
		settings.setString(sid, Acceptor.SETTING_SOCKET_ACCEPT_PORT, Integer.toString(port));
		settings.setString(sid, "UseDataDictionary", "N");
		settings.setString(sid, "HeartBtInt", "30");
		settings.setString(sid, "StartTime", "00:00:00");
		settings.setString(sid, "EndTime", "23:59:59");

		MessageStoreFactory storeFactory = new MemoryStoreFactory();
		LogFactory logFactory = new ScreenLogFactory(true, true, true);
		MessageFactory messageFactory = new DefaultMessageFactory();
		Acceptor acceptor = new ThreadedSocketAcceptor(app, storeFactory, settings, logFactory, messageFactory);
		acceptor.start();
		return acceptor;
	}

	static class HarnessApp extends MessageCracker implements Application {
		private final CountDownLatch appLatch = new CountDownLatch(1);
		private volatile Message lastApp;

		Message awaitAppMessage(long time, TimeUnit unit) throws InterruptedException {
			if (!appLatch.await(time, unit)) return null;
			return lastApp;
		}

		@Override public void onCreate(SessionID sessionId) {}
		@Override public void onLogon(SessionID sessionId) {}
		@Override public void onLogout(SessionID sessionId) {}
		@Override public void toAdmin(Message message, SessionID sessionId) {}
		@Override public void fromAdmin(Message message, SessionID sessionId) {}
		@Override public void toApp(Message message, SessionID sessionId) {}
		@Override public void fromApp(Message message, SessionID sessionId) { lastApp = message; appLatch.countDown(); }
	}
}
