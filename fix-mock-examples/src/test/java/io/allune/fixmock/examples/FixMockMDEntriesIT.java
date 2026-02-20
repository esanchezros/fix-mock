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
import io.allune.fixmock.matching.FixMessagePatternBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.MarketDataSnapshotFullRefresh;

import java.time.LocalDateTime;
import java.util.Properties;

import static io.allune.fixmock.FixMock.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Market Data example: MDEntries(268) group with numeric predicates on MDEntryPx(270) / MDEntrySize(271).
 */
public class FixMockMDEntriesIT {

	private FixMockServer server;
	private Initiator initiator;

	@BeforeEach void setUp() { server = null; initiator = null; }
	@AfterEach void tearDown() throws Exception { if (initiator != null) initiator.stop(); if (server != null) server.stop(); }

	@Test
	void verifies_mdentries_numeric_predicates() throws Exception {
		int port = 9936;
		FixMockServerOptions options = FixMockServerOptions.builder()
				.beginString("FIX.4.4")
				.senderCompId("BROKER")
				.targetCompId("CLIENT")
				.acceptorPort(port)
				.useDataDictionary(true)
				.build();
		server = new SimpleFixMockServer(options);
		server.start();
		assertTrue(server.isRunning());

		Application app = new MDSnapshotOnLogon();
		SessionSettings settings = initiatorSettings(port);
		MessageStoreFactory storeFactory = new MemoryStoreFactory();
		LogFactory logFactory = new ScreenLogFactory(true, true, true);
		MessageFactory messageFactory = new DefaultMessageFactory();
		initiator = new SocketInitiator(app, storeFactory, settings, logFactory, messageFactory);
		initiator.start();
		// Allow time for logon and app message to be sent
		Thread.sleep(1500);

		// Verify we received a MarketDataSnapshot (W) that includes MDEntryPx(270)>100 and MDEntrySize(271) in [10, 1000]
		// Using name-aware group selector and tag name predicates
		server.verify(
				atLeast(1),
				appMessageRequestedFor()
						.messageType("W")
						.withGroup("MDEntries", FixMessagePatternBuilder.GroupElementSelector.anyElement()
								.withTagName("MDEntryType", equalTo("0")) // Bid
								.withTagName("MDEntryPx", greaterThan(100))
								.withTagName("MDEntrySize", between(10, 1000))
						)
		);
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
		settings.setString(sessionID, "StartTime", "00:00:00");
		settings.setString(sessionID, "EndTime", "23:59:59");
		return settings;
	}

	/** Sends a MarketDataSnapshotFullRefresh (W) with one MDEntry on logon. */
	static class MDSnapshotOnLogon extends MessageCracker implements Application {
		@Override public void onCreate(SessionID sessionId) {}
		@Override public void onLogon(SessionID sessionId) {
			try {
				MarketDataSnapshotFullRefresh snap = new MarketDataSnapshotFullRefresh();
				snap.set(new Symbol("MSFT"));
				// Add one MDEntry: type BID (0), price 123.45, size 100
				snap.setInt(268, 1); // NoMDEntries
				quickfix.Group mdEntry = new quickfix.Group(268, 269);
				mdEntry.setChar(269, '0');		  // MDEntryType
				mdEntry.setDouble(270, 123.45);	 // MDEntryPx
				mdEntry.setDouble(271, 100);		// MDEntrySize
				// Add an update time for realism
				snap.addGroup(mdEntry);
				Session.sendToTarget(snap, sessionId);
			} catch (Exception ignored) {}
		}
		@Override public void onLogout(SessionID sessionId) {}
		@Override public void toAdmin(Message message, SessionID sessionId) {}
		@Override public void fromAdmin(Message message, SessionID sessionId) {}
		@Override public void toApp(Message message, SessionID sessionId) {}
		@Override public void fromApp(Message message, SessionID sessionId) {}
	}
}
