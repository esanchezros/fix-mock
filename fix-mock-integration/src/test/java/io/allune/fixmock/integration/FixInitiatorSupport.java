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
package io.allune.fixmock.integration;

import quickfix.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Reusable helper to create QuickFIX/J initiators, session settings, and capturing applications.
 * Eliminates boilerplate repeated across every integration test.
 */
final class FixInitiatorSupport {

	private FixInitiatorSupport() {}

	/**
	 * Creates standard initiator SessionSettings for connecting to a local mock server.
	 */
	static SessionSettings initiatorSettings(String sender, String target, int port) throws ConfigError {
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

	/**
	 * Creates and starts a SocketInitiator with the given Application.
	 */
	static Initiator startInitiator(Application app, String sender, String target, int port) throws ConfigError {
		SessionSettings settings = initiatorSettings(sender, target, port);
		MessageStoreFactory storeFactory = new MemoryStoreFactory();
		LogFactory logFactory = new ScreenLogFactory(true, true, true);
		MessageFactory messageFactory = new DefaultMessageFactory();
		Initiator initiator = new SocketInitiator(app, storeFactory, settings, logFactory, messageFactory);
		initiator.start();
		return initiator;
	}

	/**
	 * Creates and starts a SocketInitiator with SubIDs for sessions requiring SenderSubID/TargetSubID.
	 */
	static Initiator startInitiator(Application app, String sender, String senderSubId,
									String target, String targetSubId, int port) throws ConfigError {
		Properties defaults = new Properties();
		defaults.setProperty(SessionFactory.SETTING_CONNECTION_TYPE, "initiator");
		defaults.setProperty(Initiator.SETTING_RECONNECT_INTERVAL, "2");
		defaults.setProperty("UseDataDictionary", "N");
		defaults.setProperty("HeartBtInt", "30");
		defaults.setProperty("StartTime", "00:00:00");
		defaults.setProperty("EndTime", "23:59:59");

		SessionID sessionID = new SessionID("FIX.4.4", sender, senderSubId, target, targetSubId);

		SessionSettings settings = new SessionSettings();
		settings.set(defaults);
		settings.setString(sessionID, SessionFactory.SETTING_CONNECTION_TYPE, "initiator");
		settings.setString(sessionID, SocketInitiator.SETTING_SOCKET_CONNECT_HOST, "127.0.0.1");
		settings.setString(sessionID, SocketInitiator.SETTING_SOCKET_CONNECT_PORT, Integer.toString(port));
		settings.setString(sessionID, "UseDataDictionary", "N");
		settings.setString(sessionID, "HeartBtInt", "30");
		settings.setString(sessionID, "StartTime", "00:00:00");
		settings.setString(sessionID, "EndTime", "23:59:59");

		MessageStoreFactory storeFactory = new MemoryStoreFactory();
		LogFactory logFactory = new ScreenLogFactory(true, true, true);
		MessageFactory messageFactory = new DefaultMessageFactory();
		Initiator initiator = new SocketInitiator(app, storeFactory, settings, logFactory, messageFactory);
		initiator.start();
		return initiator;
	}

	/**
	 * Application that sends a single FIX message upon logon and captures all received app messages.
	 * The message to send is provided at construction time.
	 */
	static class SendOnceAndCaptureApp extends MessageCracker implements Application {
		private final Message messageToSend;
		private final CopyOnWriteArrayList<Message> received = new CopyOnWriteArrayList<>();
		private final CountDownLatch responseLatch;
		private volatile boolean sent = false;

		SendOnceAndCaptureApp(Message messageToSend) {
			this(messageToSend, 1);
		}

		SendOnceAndCaptureApp(Message messageToSend, int expectedResponses) {
			this.messageToSend = messageToSend;
			this.responseLatch = new CountDownLatch(expectedResponses);
		}

		Message awaitFirstResponse(long timeout, TimeUnit unit) throws InterruptedException {
			if (!responseLatch.await(timeout, unit)) return null;
			return received.isEmpty() ? null : received.get(0);
		}

		List<Message> awaitResponses(long timeout, TimeUnit unit) throws InterruptedException {
			responseLatch.await(timeout, unit);
			return new ArrayList<>(received);
		}

		List<Message> getReceived() {
			return new ArrayList<>(received);
		}

		@Override public void onCreate(SessionID sessionId) {}

		@Override
		public void onLogon(SessionID sessionId) {
			if (!sent) {
				try {
					Session.sendToTarget(messageToSend, sessionId);
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

		@Override
		public void fromApp(Message message, SessionID sessionId) {
			received.add(message);
			responseLatch.countDown();
		}
	}

	/**
	 * Application that sends multiple FIX messages in sequence upon logon and captures all responses.
	 */
	static class SendMultipleAndCaptureApp extends MessageCracker implements Application {
		private final List<Message> messagesToSend;
		private final CopyOnWriteArrayList<Message> received = new CopyOnWriteArrayList<>();
		private final CountDownLatch responseLatch;
		private volatile boolean sent = false;

		SendMultipleAndCaptureApp(List<Message> messagesToSend, int expectedResponses) {
			this.messagesToSend = new ArrayList<>(messagesToSend);
			this.responseLatch = new CountDownLatch(expectedResponses);
		}

		List<Message> awaitResponses(long timeout, TimeUnit unit) throws InterruptedException {
			responseLatch.await(timeout, unit);
			return new ArrayList<>(received);
		}

		List<Message> getReceived() {
			return new ArrayList<>(received);
		}

		@Override public void onCreate(SessionID sessionId) {}

		@Override
		public void onLogon(SessionID sessionId) {
			if (!sent) {
				try {
					for (Message msg : messagesToSend) {
						Session.sendToTarget(msg, sessionId);
					}
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

		@Override
		public void fromApp(Message message, SessionID sessionId) {
			received.add(message);
			responseLatch.countDown();
		}
	}

	/**
	 * Application that only captures received app messages (does not send anything on logon).
	 */
	static class CaptureOnlyApp extends MessageCracker implements Application {
		private final CopyOnWriteArrayList<Message> received = new CopyOnWriteArrayList<>();
		private final CountDownLatch responseLatch;

		CaptureOnlyApp(int expectedMessages) {
			this.responseLatch = new CountDownLatch(expectedMessages);
		}

		List<Message> awaitResponses(long timeout, TimeUnit unit) throws InterruptedException {
			responseLatch.await(timeout, unit);
			return new ArrayList<>(received);
		}

		List<Message> getReceived() {
			return new ArrayList<>(received);
		}

		@Override public void onCreate(SessionID sessionId) {}
		@Override public void onLogon(SessionID sessionId) {}
		@Override public void onLogout(SessionID sessionId) {}
		@Override public void toAdmin(Message message, SessionID sessionId) {}
		@Override public void fromAdmin(Message message, SessionID sessionId) {}
		@Override public void toApp(Message message, SessionID sessionId) {}

		@Override
		public void fromApp(Message message, SessionID sessionId) {
			received.add(message);
			responseLatch.countDown();
		}
	}
}
