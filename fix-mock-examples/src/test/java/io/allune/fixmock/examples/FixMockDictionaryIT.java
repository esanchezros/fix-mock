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

import static io.allune.fixmock.FixMock.messageType;
import static io.allune.fixmock.FixMock.within;
import static io.allune.fixmock.FixMock.atLeast;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test for dictionary-aware parsing using the bundled FIX44.xml by default.
 */
public class FixMockDictionaryIT {

	private FixMockServer server;
	private Initiator initiator;

	@BeforeEach
	void setUp() { server = null; initiator = null; }

	@AfterEach
	void tearDown() throws Exception {
		if (initiator != null) initiator.stop();
		if (server != null) server.stop();
	}

	@Test
	void server_with_dictionary_enabled_accepts_messages() throws Exception {
		int port = 9930;
		FixMockServerOptions opts = FixMockServerOptions.builder()
				.role(FixMockServerOptions.Role.ACCEPTOR)
				.beginString("FIX.4.4")
				.senderCompId("BROKER")
				.targetCompId("CLIENT")
				.acceptorPort(port)
				.useDataDictionary(true) // no explicit path: defaults to FIX44.xml bundled
				.build();

		server = new SimpleFixMockServer(opts);
		server.start();
		assertTrue(server.isRunning());

		// Initiator sends a valid FIX44 NewOrderSingle that should validate against dictionary
		Application app = new AppThatSendsOnce();
		SessionSettings settings = initiatorSettings(port);
		MessageStoreFactory storeFactory = new MemoryStoreFactory();
		LogFactory logFactory = new ScreenLogFactory(true, true, true);
		MessageFactory messageFactory = new DefaultMessageFactory();
		initiator = new SocketInitiator(app, storeFactory, settings, logFactory, messageFactory);
		initiator.start();

		Thread.sleep(1500);

		server.verify(atLeast(1), within(Duration.ofSeconds(5)), messageType("D"));
	}

	private static SessionSettings initiatorSettings(int port) throws ConfigError {
		Properties defaults = new Properties();
		defaults.setProperty(SessionFactory.SETTING_CONNECTION_TYPE, "initiator");
		defaults.setProperty(Initiator.SETTING_RECONNECT_INTERVAL, "2");
		defaults.setProperty("UseDataDictionary", "Y");
		defaults.setProperty("HeartBtInt", "30");
		defaults.setProperty("StartTime", "00:00:00");
		defaults.setProperty("EndTime", "23:59:59");

		SessionID sessionID = new SessionID("FIX.4.4", "CLIENT", "BROKER");

		SessionSettings settings = new SessionSettings();
		settings.set(defaults);
		settings.setString(sessionID, SessionFactory.SETTING_CONNECTION_TYPE, "initiator");
		settings.setString(sessionID, SocketInitiator.SETTING_SOCKET_CONNECT_HOST, "127.0.0.1");
		settings.setString(sessionID, SocketInitiator.SETTING_SOCKET_CONNECT_PORT, Integer.toString(port));
		settings.setString(sessionID, "UseDataDictionary", "Y");
		settings.setString(sessionID, "HeartBtInt", "30");
		// No explicit DataDictionary; QuickFIX/J should load FIX44.xml from classpath
		return settings;
	}

	static class AppThatSendsOnce extends MessageCracker implements Application {
		private volatile boolean sent = false;
		@Override public void onCreate(SessionID sessionId) {}
		@Override public void onLogon(SessionID sessionId) {
			if (!sent) {
				try {
					NewOrderSingle nos = new NewOrderSingle(
							new ClOrdID(UUID.randomUUID().toString()),
							new Side(Side.BUY),
							new TransactTime(LocalDateTime.now()),
							new OrdType(OrdType.MARKET)
					);
					nos.set(new Symbol("AAPL"));
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
		@Override public void fromApp(Message message, SessionID sessionId) {}
	}
}
