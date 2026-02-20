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
import quickfix.fix44.NewOrderSingle;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;

import static io.allune.fixmock.FixMock.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Repeating groups example: verify Parties(453) contains a PartyID element.
 */
public class FixMockPartiesGroupIT {

	private FixMockServer server;
	private Initiator initiator;

	@BeforeEach void setUp() { server = null; initiator = null; }
	@AfterEach void tearDown() throws Exception { if (initiator != null) initiator.stop(); if (server != null) server.stop(); }

	@Test
	void verifies_parties_group_contains_expected_partyid() throws Exception {
		int port = 9935;
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

		// Initiator app that sends a NOS with Parties group on logon
		Application app = new PartiesNosOnLogon("CLIENT-XYZ");
		SessionSettings settings = initiatorSettings(port);
		MessageStoreFactory storeFactory = new MemoryStoreFactory();
		LogFactory logFactory = new ScreenLogFactory(true, true, true);
		MessageFactory messageFactory = new DefaultMessageFactory();
		initiator = new SocketInitiator(app, storeFactory, settings, logFactory, messageFactory);
		initiator.start();
		// Allow time for logon and app message to be sent
		Thread.sleep(1500);

		// Verify Parties group contains PartyID(448)=CLIENT-XYZ via group selector (name-aware)
		server.verify(
				atLeast(1),
				appMessageRequestedFor()
						.messageType("D")
						.withGroup("Parties", FixMessagePatternBuilder.GroupElementSelector.anyElement()
								.withTagName("PartyID", equalTo("CLIENT-XYZ"))
						)
		);
	}

	private static SessionSettings initiatorSettings(int port) throws ConfigError {
		Properties defaults = new Properties();
		defaults.setProperty(SessionFactory.SETTING_CONNECTION_TYPE, "initiator");
		defaults.setProperty(Initiator.SETTING_RECONNECT_INTERVAL, "2");
		// Keep dictionary disabled on the live session; verification parsing uses its own dictionary
		defaults.setProperty("UseDataDictionary", "N");
		defaults.setProperty("HeartBtInt", "30");
		defaults.setProperty("StartTime", "00:00:00");
		defaults.setProperty("EndTime", "23:59:59");

		SessionID sessionID = new SessionID("FIX.4.4", "CLIENT", "BROKER");

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

	/** Sends a NOS with Parties group on logon. */
	static class PartiesNosOnLogon extends MessageCracker implements Application {
		private final String partyId;
		PartiesNosOnLogon(String partyId) { this.partyId = partyId; }
		@Override public void onCreate(SessionID sessionId) {}
		@Override public void onLogon(SessionID sessionId) {
			try {
				NewOrderSingle nos = new NewOrderSingle(
						new ClOrdID(UUID.randomUUID().toString()),
						new Side(Side.BUY),
						new TransactTime(LocalDateTime.now()),
						new OrdType(OrdType.MARKET)
				);
				nos.set(new Symbol("AAPL"));

				// Parties group: set count NoPartyIDs(453)=1, then add the group element with PartyID(448)
				nos.setInt(453, 1);
				quickfix.Group party = new quickfix.Group(453, 448);
				party.setString(448, partyId); // PartyID
				// Optionally set source/role if desired
				// party.setChar(447, 'D');
				// party.setInt(452, 1);
				nos.addGroup(party);

				Session.sendToTarget(nos, sessionId);
			} catch (Exception ignored) {}
		}
		@Override public void onLogout(SessionID sessionId) {}
		@Override public void toAdmin(Message message, SessionID sessionId) {}
		@Override public void fromAdmin(Message message, SessionID sessionId) {}
		@Override public void toApp(Message message, SessionID sessionId) {}
		@Override public void fromApp(Message message, SessionID sessionId) {}
	}
}
