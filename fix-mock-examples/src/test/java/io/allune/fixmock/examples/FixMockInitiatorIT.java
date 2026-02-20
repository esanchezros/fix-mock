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
import io.allune.fixmock.server.SimpleFixMockServer;
import io.allune.fixmock.verify.CountMatchingStrategy;
import io.allune.fixmock.verify.TimeWindow;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import quickfix.*;
import quickfix.field.ClOrdID;
import quickfix.field.OrdType;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;
import quickfix.fix44.NewOrderSingle;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;

import static io.allune.fixmock.verify.CountMatchingStrategy.atLeast;
import static io.allune.fixmock.examples.Patterns.messageType;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration-style test: start FixMock as Acceptor, run an Initiator that sends a message,
 * then verify the server journal saw it with the expected contents using FixMock's verify().
 */
public class FixMockInitiatorIT {

	private static FixMockServer server;
	private static Initiator initiator;

	@BeforeAll
	static void startServer() {
		// Start FixMock as Acceptor (server side)
		server = new SimpleFixMockServer("FIX.4.4", "BROKER", "CLIENT", 9899);
		server.start();
		assertTrue(server.isRunning(), "Server should be running");
	}

	@AfterAll
	static void stopAll() throws Exception {
		if (initiator != null) {
			initiator.stop();
		}
		if (server != null) {
			server.stop();
		}
	}

	@Test
	void initiatorSendsNewOrderSingle_and_serverVerifiesReceipt() throws Exception {
		// Configure and start a minimal QuickFIX/J Initiator
		Application app = new AppThatSendsOnce();
		SessionSettings settings = initiatorSettings();
		MessageStoreFactory storeFactory = new MemoryStoreFactory();
		LogFactory logFactory = new ScreenLogFactory(true, true, true);
		MessageFactory messageFactory = new DefaultMessageFactory();
		initiator = new SocketInitiator(app, storeFactory, settings, logFactory, messageFactory);
		initiator.start();

		// Wait up to a few seconds for logon and message exchange
		Thread.sleep(2000);

		// Verify on the server side: at least 1 message with 35=D and tag 55=AAPL within 5 seconds
		CountMatchingStrategy count = atLeast(1);
		TimeWindow window = TimeWindow.within(Duration.ofSeconds(5));
		server.verify(count, window, messageType("D").withTagEquals(55, "AAPL"));
	}

	private static SessionSettings initiatorSettings() throws ConfigError {
		Properties defaults = new Properties();
		defaults.setProperty(SessionFactory.SETTING_CONNECTION_TYPE, "initiator");
		defaults.setProperty(Initiator.SETTING_RECONNECT_INTERVAL, "2");
		defaults.setProperty("UseDataDictionary", "N");
		defaults.setProperty("HeartBtInt", "30");
		defaults.setProperty("StartTime", "00:00:00");
		defaults.setProperty("EndTime", "23:59:59");

		SessionID sessionID = new SessionID("FIX.4.4", "CLIENT", "BROKER");

		SessionSettings settings = new SessionSettings();
		settings.set(defaults);
		settings.setString(sessionID, SessionFactory.SETTING_CONNECTION_TYPE, "initiator");
		settings.setString(sessionID, SocketInitiator.SETTING_SOCKET_CONNECT_HOST, "127.0.0.1");
		settings.setString(sessionID, SocketInitiator.SETTING_SOCKET_CONNECT_PORT, "9899");
		settings.setString(sessionID, "UseDataDictionary", "N");
		settings.setString(sessionID, "HeartBtInt", "30");
		settings.setString(sessionID, "StartTime", "00:00:00");
		settings.setString(sessionID, "EndTime", "23:59:59");
		return settings;
	}

	/** Initiator app that sends one NewOrderSingle upon logon. */
	static class AppThatSendsOnce extends MessageCracker implements Application {
		private volatile boolean sent = false;

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
					nos.set(new Symbol("AAPL"));
					Session.sendToTarget(nos, sessionId);
					sent = true;
				} catch (SessionNotFound e) {
					throw new RuntimeException(e);
				}
			}
		}

		@Override
		public void onLogout(SessionID sessionId) {}

		@Override
		public void toAdmin(Message message, SessionID sessionId) {}

		@Override
		public void fromAdmin(Message message, SessionID sessionId) {}

		@Override
		public void toApp(Message message, SessionID sessionId) {}

		@Override
		public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectTagValue, UnsupportedMessageType {
			// Not needed for this test
		}
	}
}
