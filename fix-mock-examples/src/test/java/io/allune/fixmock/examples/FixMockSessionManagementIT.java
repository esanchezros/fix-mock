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
import quickfix.field.MsgType;
import quickfix.field.TestReqID;
import quickfix.fix44.Heartbeat;
import quickfix.fix44.Logout;
import quickfix.fix44.TestRequest;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.allune.fixmock.FixMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Session management flow: Logon → TestRequest → Heartbeat → Logout.
 *
 * This test runs the server as an acceptor and a minimal initiator app that
 * triggers a TestRequest on logon, waits for Heartbeat (0), then requests Logout (5).
 * We assert that the initiator receives Heartbeat and Logout admin messages.
 */
public class FixMockSessionManagementIT {

	private FixMockServer server;
	private Initiator initiator;

	@BeforeEach void setUp() { server = null; initiator = null; }
	@AfterEach void tearDown() throws Exception { if (initiator != null) initiator.stop(); if (server != null) server.stop(); }

	@Test
	void session_management_logon_testrequest_heartbeat_logout() throws Exception {
		int port = 9917;
		server = new SimpleFixMockServer("FIX.4.4", "BROKER", "CLIENT", port);
		server.start();
		assertTrue(server.isRunning());

		SessionFlowApp app = new SessionFlowApp();
		initiator = startInitiator(app, "CLIENT", "BROKER", port);

		boolean completed = app.awaitAdminFlow(5, TimeUnit.SECONDS);
		assertTrue(completed, "Expected Heartbeat and Logout within timeout");
		assertTrue(app.receivedHeartbeat(), "Should receive Heartbeat (35=0)");
		assertTrue(app.receivedLogout(), "Should receive Logout (35=5)");
	}

	@Test
	void explicit_admin_stubbing_test_request() throws Exception {
		int port = 9918;
		server = new SimpleFixMockServer("FIX.4.4", "BROKER", "CLIENT", port);
		server.start();

		// Stub explicit TestRequest(35=1) -> Heartbeat(35=0) with custom TestReqID
		String testReqId = "CUSTOM-ID";
		server.stubFor(when(adminMessageRequestedFor().messageType("1"))
				.willReturn(testRequestResponse(testReqId)));

		SessionFlowApp app = new SessionFlowApp();
		initiator = startInitiator(app, "CLIENT", "BROKER", port);

		boolean completed = app.awaitAdminFlow(5, TimeUnit.SECONDS);
		assertTrue(completed, "Expected Heartbeat within timeout");
		
		// The CapturingApp should have received Heartbeat with our custom testReqId
		// We can verify it in the mock journal too
		server.verify(exactly(1), adminMessageRequestedFor().messageType("0").withTag(112, testReqId));
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

	/** Initiator app that performs a TestRequest on logon and logs out after receiving Heartbeat. */
	static class SessionFlowApp extends MessageCracker implements Application {
		private final CountDownLatch heartbeatLatch = new CountDownLatch(1);
		private final CountDownLatch logoutLatch = new CountDownLatch(1);

		boolean receivedHeartbeat() { return heartbeatLatch.getCount() == 0; }
		boolean receivedLogout() { return logoutLatch.getCount() == 0; }
		boolean awaitAdminFlow(long time, TimeUnit unit) throws InterruptedException {
			long deadline = System.nanoTime() + unit.toNanos(time);
			long remaining;
			remaining = deadline - System.nanoTime();
			if (remaining <= 0) return false;
			if (!heartbeatLatch.await(remaining, TimeUnit.NANOSECONDS)) return false;
			remaining = deadline - System.nanoTime();
			if (remaining <= 0) return false;
			return logoutLatch.await(remaining, TimeUnit.NANOSECONDS);
		}

		@Override public void onCreate(SessionID sessionId) {}
		@Override public void onLogon(SessionID sessionId) {
			try {
				// Send a TestRequest to trigger Heartbeat(0) from the acceptor
				TestRequest tr = new TestRequest();
				tr.set(new TestReqID(UUID.randomUUID().toString()));
				Session.sendToTarget(tr, sessionId);
			} catch (Exception ignored) {}
		}
		@Override public void onLogout(SessionID sessionId) {}
		@Override public void toAdmin(Message message, SessionID sessionId) {}
		@Override public void fromAdmin(Message message, SessionID sessionId) {
			try {
				String mt = message.getHeader().getString(MsgType.FIELD);
				if (Heartbeat.MSGTYPE.equals(mt) || "0".equals(mt)) {
					heartbeatLatch.countDown();
					// After heartbeat, initiate a Logout request
					Session.lookupSession(sessionId).logout("bye");
				} else if (Logout.MSGTYPE.equals(mt) || "5".equals(mt)) {
					logoutLatch.countDown();
				}
			} catch (FieldNotFound ignored) {}
		}
		@Override public void toApp(Message message, SessionID sessionId) {}
		@Override public void fromApp(Message message, SessionID sessionId) {}
	}
}
