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
import quickfix.fix44.NewOrderSingle;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;

import static io.allune.fixmock.FixMock.*;
import static org.junit.jupiter.api.Assertions.*;

public class FixMockRichFailureIT {

	private FixMockServer server;
	private Initiator initiator;

	@BeforeEach void setUp() { server = null; initiator = null; }
	@AfterEach void tearDown() throws Exception { if (initiator != null) initiator.stop(); if (server != null) server.stop(); }

	@Test
	void failure_shows_tag_level_diff() throws Exception {
		int port = 9940;
		server = new SimpleFixMockServer("FIX.4.4", "BROKER", "CLIENT", port);
		server.start();

		// Initiator sends a NOS for MSFT
		initiator = startInitiator("CLIENT", "BROKER", port, "MSFT");
		assertTrue(((SimpleFixMockServer) server).awaitLogon(java.time.Duration.ofSeconds(5)), "Should log on");
		Thread.sleep(1000);

		// We verify for AAPL
		AssertionError error = assertThrows(AssertionError.class, () -> 
			server.verify(exactly(1), appMessageRequestedFor()
				.messageType("D")
				.withTagName("Symbol", equalTo("AAPL")))
		);

		String msg = error.getMessage();

		assertTrue(msg.contains("Verification failed"), "Should contain failure header");
		assertTrue(msg.contains("Closest match found"), "Should identify closest match");
		assertTrue(msg.contains("[X] Symbol: expected equalTo(\"AAPL\"), but was MSFT"), "Should show tag diff");
		assertTrue(msg.contains("[✓] MsgType (35): D"), "Should show matched fields");
		assertTrue(msg.contains("Full message:"), "Should show pretty-printed message");
	}

	private static Initiator startInitiator(String sender, String target, int port, String symbol) throws ConfigError, InterruptedException {
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

		Application app = new Application() {
			@Override public void onCreate(SessionID sessionId) {}
			@Override public void onLogon(SessionID sessionId) {
				try {
					NewOrderSingle nos = new NewOrderSingle(
							new ClOrdID(UUID.randomUUID().toString()),
							new Side(Side.BUY),
							new TransactTime(LocalDateTime.now()),
							new OrdType(OrdType.MARKET)
					);
					nos.set(new Symbol(symbol));
					Session.sendToTarget(nos, sessionId);
				} catch (Exception ignored) {}
			}
			@Override public void onLogout(SessionID sessionId) {}
			@Override public void toAdmin(Message message, SessionID sessionId) {}
			@Override public void fromAdmin(Message message, SessionID sessionId) {}
			@Override public void toApp(Message message, SessionID sessionId) {}
			@Override public void fromApp(Message message, SessionID sessionId) {}
		};

		Initiator initiator = new SocketInitiator(app, new MemoryStoreFactory(), settings, new ScreenLogFactory(true, true, true), new DefaultMessageFactory());
		initiator.start();
		return initiator;
	}
}
