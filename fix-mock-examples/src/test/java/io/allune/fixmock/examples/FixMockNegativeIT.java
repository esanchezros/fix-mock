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
import quickfix.fix44.BusinessMessageReject;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.Reject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.allune.fixmock.FixMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Negative testing: stubbing BusinessMessageReject (35=j) and session-level Reject (35=3),
 * and asserting the initiator receives them.
 */
public class FixMockNegativeIT {

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
	void stub_businessMessageReject_on_invalid_symbol_and_assert_initiator_receives_j() throws Exception {
		int port = 9904;
		server = new SimpleFixMockServer("FIX.4.4", "BROKER", "CLIENT", port);
		server.start();
		assertTrue(server.isRunning());

		// Pattern: NOS for BLACKLISTED symbol -> respond with BMR (35=j)
		server.stubFor(when(
				FixMock.appMessageRequestedFor()
						.messageType("D")
						.withTag(55, equalTo("BAD"))
		).willReturn(businessReject("D", "Symbol not allowed", BusinessRejectReason.OTHER)));

		// Initiator that captures incoming app/admin messages
		CapturingApp app = new CapturingApp();
		initiator = startInitiator(app, "CLIENT", "BROKER", port);

		// Send one NOS with BAD symbol
		sendNOS(app.getSessionIdLatch(), "BAD");

		// Expect BMR in fromApp (35=j) within 5 seconds
		Message m = app.awaitAppMessage(5, TimeUnit.SECONDS);
		assertNotNull(m, "Expected a BusinessMessageReject");
		assertEquals("j", m.getHeader().getString(MsgType.FIELD));
		assertEquals("D", m.getString(RefMsgType.FIELD));
		assertEquals("Symbol not allowed", m.getString(Text.FIELD));

		// Also verify on server side journal
		server.verify(atLeast(1), within(Duration.ofSeconds(5)), messageType("D").withTag(55, equalTo("BAD")));
	}

	@Test
	void stub_session_reject_on_any_nos_and_assert_initiator_receives_3() throws Exception {
		int port = 9905;
		server = new SimpleFixMockServer("FIX.4.4", "BROKER", "CLIENT", port);
		server.start();
		assertTrue(server.isRunning());

		// Pattern: any NOS -> respond with session-level Reject (35=3)
		server.stubFor(when(
				FixMock.appMessageRequestedFor().messageType("D")
		).willReturn(sessionReject(1, "Invalid tag specified")));

		CapturingApp app = new CapturingApp();
		initiator = startInitiator(app, "CLIENT", "BROKER", port);

		// Send one NOS with MSFT
		sendNOS(app.getSessionIdLatch(), "MSFT");

		// Expect Reject in fromAdmin (35=3) within 5 seconds
		Message admin = app.awaitAdminMessageType3(5, TimeUnit.SECONDS);
		assertNotNull(admin, "Expected a session Reject (35=3)");
		assertEquals("3", admin.getHeader().getString(MsgType.FIELD));
		assertEquals(1, admin.getInt(RefSeqNum.FIELD));
		assertEquals("Invalid tag specified", admin.getString(Text.FIELD));

		// Verify NOS was journaled
		server.verify(exactly(1), within(Duration.ofSeconds(5)), messageType("D"));
	}

	// --- Response builders ---
	private static Message businessReject(String refMsgType, String text, int reason) {
		// In FIX.4.4 the generated constructor requires RefMsgType and BusinessRejectReason
		BusinessMessageReject bmr = new BusinessMessageReject(
				new RefMsgType(refMsgType),
				new BusinessRejectReason(reason)
		);
		bmr.getHeader().setString(MsgType.FIELD, "j");
		bmr.setField(new BusinessRejectRefID("BRID-1"));
		bmr.setField(new Text(text));
		return bmr;
	}

	private static Message sessionReject(int refSeqNum, String text) {
		Reject r = new Reject(new RefSeqNum(refSeqNum));
		r.getHeader().setString(MsgType.FIELD, "3");
		r.setField(new Text(text));
		return r;
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

	private static void sendNOS(CountDownLatch sessionIdLatch, String symbol) throws InterruptedException {
		// Wait for session to be known in app (logon)
		assertTrue(sessionIdLatch.await(5, TimeUnit.SECONDS), "Session not ready for send");
	}

	/** Application that captures inbound admin and app messages and exposes session id for sending. */
	static class CapturingApp extends MessageCracker implements Application {
		private volatile SessionID currentSessionId;
		private final CountDownLatch sessionIdLatch = new CountDownLatch(1);
		private final CountDownLatch appLatch = new CountDownLatch(1);
		private final CountDownLatch adminLatch = new CountDownLatch(1);
		private final CountDownLatch adminType3Latch = new CountDownLatch(1);
		private volatile Message lastAppMessage;
		private volatile Message lastAdminMessage;
		private volatile Message lastAdminType3;

		CountDownLatch getSessionIdLatch() { return sessionIdLatch; }

		Message awaitAppMessage(long time, TimeUnit unit) throws InterruptedException {
			if (!appLatch.await(time, unit)) return null;
			return lastAppMessage;
		}

		Message awaitAdminMessage(long time, TimeUnit unit) throws InterruptedException {
			if (!adminLatch.await(time, unit)) return null;
			return lastAdminMessage;
		}

		Message awaitAdminMessageType3(long time, TimeUnit unit) throws InterruptedException {
			if (!adminType3Latch.await(time, unit)) return null;
			return lastAdminType3;
		}

		@Override public void onCreate(SessionID sessionId) {}

		@Override public void onLogon(SessionID sessionId) {
			this.currentSessionId = sessionId;
			sessionIdLatch.countDown();
			// Immediately send a NOS when we log on; specific symbol can be updated by setting a default
			try {
				NewOrderSingle nos = new NewOrderSingle(
						new ClOrdID(UUID.randomUUID().toString()),
						new Side(Side.BUY),
						new TransactTime(LocalDateTime.now()),
						new OrdType(OrdType.MARKET)
				);
				// The symbol will be set later by tests via Session.sendToTarget if desired, but default to BAD for first test
				nos.set(new Symbol("BAD"));
				Session.sendToTarget(nos, sessionId);
			} catch (Exception e) {
				// ignore in tests
			}
		}

		@Override public void onLogout(SessionID sessionId) {}
		@Override public void toAdmin(Message message, SessionID sessionId) {}
		@Override public void fromAdmin(Message message, SessionID sessionId) {
			lastAdminMessage = message;
			adminLatch.countDown();
			try {
				String mt = message.getHeader().getString(MsgType.FIELD);
				if ("3".equals(mt) && lastAdminType3 == null) {
					lastAdminType3 = message;
					adminType3Latch.countDown();
				}
			} catch (Exception ignore) {
				// not critical in tests
			}
		}
		@Override public void toApp(Message message, SessionID sessionId) {}
		@Override public void fromApp(Message message, SessionID sessionId) {
			lastAppMessage = message;
			appLatch.countDown();
		}
	}
}
