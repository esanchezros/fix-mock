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
package io.allune.fixmock.server;

import io.allune.fixmock.FixMockServer;
import io.allune.fixmock.FixMockServerOptions;
import io.allune.fixmock.SessionConfig;
import io.allune.fixmock.matching.FixMessagePrettyPrinter;
import io.allune.fixmock.record.RecordingService;
import io.allune.fixmock.stubbing.StubMapping;
import io.allune.fixmock.verify.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.BeginString;
import quickfix.field.MsgType;
import quickfix.field.SenderCompID;
import quickfix.field.TargetCompID;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * QuickFIX/J-backed implementation of FixMockServer.
 * Runs as an Acceptor, keeps an in-memory journal of received messages, and supports verification and stubbing.
 */
public final class SimpleFixMockServer implements FixMockServer {

	private static final Logger log = LoggerFactory.getLogger(SimpleFixMockServer.class);

	private String beginString;
	private String senderCompId;
	private String targetCompId;
	private int port;

	private final List<FixMockServerOptions> optionsList = new ArrayList<>(); // Support multiple options if needed, but usually one options object with multiple sessions
	private FixMockServerOptions options;

	private final List<FixJournalEntry> journal = new CopyOnWriteArrayList<>();
	private final List<StubMapping> stubs = new CopyOnWriteArrayList<>();
	private final RecordingService recordingService = new RecordingService();
	private final AtomicBoolean running = new AtomicBoolean(false);

	private final List<Acceptor> acceptors = new ArrayList<>();
	private final List<Initiator> initiators = new ArrayList<>();
	private final List<SessionID> sessionIDs = new ArrayList<>();

	public SimpleFixMockServer(String beginString, String senderCompId, String targetCompId, int port) {
		this.beginString = Objects.requireNonNull(beginString);
		this.senderCompId = Objects.requireNonNull(senderCompId);
		this.targetCompId = Objects.requireNonNull(targetCompId);
		this.port = port;
		this.options = null;
	}

	/** Preferred constructor using options builder (Option B). */
	public SimpleFixMockServer(FixMockServerOptions options) {
		this.options = Objects.requireNonNull(options, "options");
		this.beginString = options.getBeginString();
		this.senderCompId = options.getSenderCompId();
		this.targetCompId = options.getTargetCompId();
		this.port = options.getAcceptorPort() == null ? 0 : options.getAcceptorPort();
	}

	@Override
	public void start() {
		if (running.get()) return;

		if (options != null && options.getRecordMode() == FixMockServerOptions.RecordMode.PLAYBACK) {
			stubs.addAll(options.getStubs());
		}

		try {
			// Build session settings (prefer options if provided)
			String useDD = options != null && options.isUseDataDictionary() ? "Y" : "N";
			String dictPath = null;
			if ("Y".equals(useDD) && options != null) {
				dictPath = options.getDictionaryPath();
				if (dictPath == null || dictPath.isBlank()) {
					dictPath = resolveDefaultDictionary(beginString);
				}
				// FIX dictionary XML files (e.g., FIX44.xml) contain external DTD references like:
				//   <!DOCTYPE fix SYSTEM "fix.dtd">
				// Java 17+ blocks external DTD access by default as an XXE security measure.
				// QuickFIX/J's DataDictionary parser requires DTD resolution to parse these files.
				//
				// This property is only set when UseDataDictionary is explicitly enabled by the user.
				// The security risk is minimal because:
				// - Dictionary files are bundled resources or explicitly configured paths, not user-supplied
				// - This is a test mocking library, not a production FIX engine
				//
				// If this property cannot be set (e.g., SecurityManager restrictions), users can pass
				// -Djavax.xml.accessExternalDTD=all as a JVM argument.
				try {
					System.setProperty("javax.xml.accessExternalDTD", "all");
				} catch (SecurityException ignored) {
					log.debug("Could not set javax.xml.accessExternalDTD property; dictionary parsing may fail");
				}
			}
			int hb = options != null ? options.getHeartBtIntSeconds() : 30;
			boolean resetOnLogon = options == null || options.isResetOnLogon();
			boolean resetOnDisconnect = options == null || options.isResetOnDisconnect();
			boolean resetOnLogout = options == null || options.isResetOnLogout();

			List<SessionConfig> sessionsToStart = options != null ? options.getSessions() : 
					List.of(SessionConfig.builder()
							.beginString(beginString)
							.senderCompId(senderCompId)
							.targetCompId(targetCompId)
							.port(port)
							.role(FixMockServerOptions.Role.ACCEPTOR)
							.build());

			for (SessionConfig config : sessionsToStart) {
				SessionSettings settings = new SessionSettings();
				settings.setString(SessionFactory.SETTING_CONNECTION_TYPE, config.getRole().name().toLowerCase());
				if (config.getRole() == FixMockServerOptions.Role.ACCEPTOR) {
					settings.setString(Acceptor.SETTING_SOCKET_ACCEPT_PORT, Integer.toString(config.getPort()));
				} else {
					settings.setString(Initiator.SETTING_RECONNECT_INTERVAL, "2");
				}
				
				settings.setString("UseDataDictionary", useDD);
				if ("Y".equals(useDD) && dictPath != null) {
					settings.setString("DataDictionary", dictPath);
				}
				settings.setString("HeartBtInt", Integer.toString(hb));
				settings.setString("StartTime", "00:00:00");
				settings.setString("EndTime", "23:59:59");
				settings.setString(Session.SETTING_RESET_ON_LOGON, resetOnLogon ? "Y" : "N");
				settings.setString(Session.SETTING_RESET_ON_DISCONNECT, resetOnDisconnect ? "Y" : "N");
				settings.setString(Session.SETTING_RESET_ON_LOGOUT, resetOnLogout ? "Y" : "N");

				SessionID sessionID;
				if ((config.getSenderSubId() != null && !config.getSenderSubId().isBlank()) ||
						(config.getTargetSubId() != null && !config.getTargetSubId().isBlank())) {
					sessionID = new SessionID(config.getBeginString(), config.getSenderCompId(),
							config.getSenderSubId() != null ? config.getSenderSubId() : "",
							config.getTargetCompId(),
							config.getTargetSubId() != null ? config.getTargetSubId() : "");
				} else {
					sessionID = new SessionID(config.getBeginString(), config.getSenderCompId(), config.getTargetCompId());
				}
				sessionIDs.add(sessionID);
				
				settings.setString(sessionID, SessionFactory.SETTING_CONNECTION_TYPE, config.getRole().name().toLowerCase());
				if (config.getRole() == FixMockServerOptions.Role.ACCEPTOR) {
					settings.setString(sessionID, Acceptor.SETTING_SOCKET_ACCEPT_PORT, Integer.toString(config.getPort()));
				} else {
					settings.setString(sessionID, SocketInitiator.SETTING_SOCKET_CONNECT_HOST, config.getHost());
					settings.setString(sessionID, SocketInitiator.SETTING_SOCKET_CONNECT_PORT, Integer.toString(config.getPort()));
				}
				
				settings.setString(sessionID, "UseDataDictionary", useDD);
				if ("Y".equals(useDD) && dictPath != null) {
					settings.setString(sessionID, "DataDictionary", dictPath);
				}
				settings.setString(sessionID, "HeartBtInt", Integer.toString(hb));
				settings.setString(sessionID, "StartTime", "00:00:00");
				settings.setString(sessionID, "EndTime", "23:59:59");

				Application app = new JournalApplication(journal, stubs, recordingService, options);
				MessageStoreFactory storeFactory = new MemoryStoreFactory();
				LogFactory logFactory = new SLF4JLogFactory(settings);
				MessageFactory messageFactory = new DefaultMessageFactory();

				if (config.getRole() == FixMockServerOptions.Role.INITIATOR) {
					SocketInitiator initiator = new SocketInitiator(app, storeFactory, settings, logFactory, messageFactory);
					initiator.start();
					initiators.add(initiator);
					log.info("SimpleFixMockServer (initiator) connecting to {}:{} for session {}", config.getHost(), config.getPort(), sessionID);
				} else {
					ThreadedSocketAcceptor acceptor = new ThreadedSocketAcceptor(app, storeFactory, settings, logFactory, messageFactory);
					acceptor.start();
					acceptors.add(acceptor);
					log.info("SimpleFixMockServer started on port {} for session {}", config.getPort(), sessionID);
				}
			}
			running.set(true);
		} catch (ConfigError e) {
			throw new RuntimeException("Failed to start SimpleFixMockServer", e);
		}
	}

	@Override
	public List<FixJournalEntry> getJournalSnapshot() {
		return Collections.unmodifiableList(new ArrayList<>(journal));
	}

	@Override
	public int clearJournal() {
		int n = journal.size();
		journal.clear();
		return n;
	}

	@Override
	public int clearStubs() {
		int n = stubs.size();
		stubs.clear();
		recordingService.clear();
		return n;
	}

	@Override
	public List<StubMapping> getRecordedStubs() {
		return recordingService.getRecordedStubs();
	}

	/** Resolve a default dictionary resource/file path based on BeginString when none is provided. */
	private String resolveDefaultDictionary(String beginString) {
		if (beginString == null) return null;
		return switch (beginString) {
			case "FIX.4.2" -> "FIX42.xml";
			case "FIX.4.4" -> "FIX44.xml";
			case "FIX.5.0SP2", "FIXT.1.1" -> "FIX50SP2.xml"; // basic heuristic
			default -> null;
		};
	}

	@Override
	public void stop() {
		if (!running.get()) return;
		try {
			for (Acceptor acceptor : acceptors) {
				try {
					acceptor.stop();
				} catch (Exception e) {
					log.warn("Error stopping acceptor: {}", e.getMessage());
				}
			}
			for (Initiator initiator : initiators) {
				try {
					initiator.stop();
				} catch (Exception e) {
					log.warn("Error stopping initiator: {}", e.getMessage());
				}
			}
			acceptors.clear();
			initiators.clear();
			sessionIDs.clear();
			log.info("FixMock server stopped");
		} finally {
			running.set(false);
		}
	}

	@Override
	public boolean isRunning() {
		return running.get();
	}

	@Override
	public void send(Message message) {
		if (sessionIDs.isEmpty()) {
			throw new IllegalStateException("No sessions configured");
		}
		send(message, sessionIDs.get(0));
	}

	@Override
	public void send(Message message, SessionID sessionId) {
		if (!running.get()) {
			throw new IllegalStateException("Server not running");
		}
		try {
			Session.sendToTarget(message, sessionId);
		} catch (SessionNotFound e) {
			throw new RuntimeException("Session not found for send: " + sessionId, e);
		}
	}

	/** Test helper: waits up to the given timeout for the session to be logged on. */
	public boolean awaitLogon(java.time.Duration timeout) {
		long deadline = System.currentTimeMillis() + (timeout == null ? 0 : timeout.toMillis());
		while (System.currentTimeMillis() < deadline) {
			try {
				if (!sessionIDs.isEmpty()) {
					boolean allLoggedOn = true;
					for (SessionID sid : sessionIDs) {
						Session session = Session.lookupSession(sid);
						if (session == null || !session.isLoggedOn()) {
							allLoggedOn = false;
							break;
						}
					}
					if (allLoggedOn) return true;
				}
				Thread.sleep(50);
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return false;
	}

	@Override
	public void verify(CountMatchingStrategy count, FixMessagePattern pattern) {
		List<FixJournalEntry> entries = journal.stream().collect(Collectors.toList());
		long matches = entries.stream().filter(pattern::test).count();
		if (!count.matches(matches)) {
			throwVerificationError(count, matches, pattern, entries, null);
		}
	}

	@Override
	public void verify(CountMatchingStrategy count, TimeWindow window, FixMessagePattern pattern) {
		long cutoff = Instant.now().minus(window.getDuration()).toEpochMilli();
		List<FixJournalEntry> entries = journal.stream()
				.filter(e -> e.getTimestampMillis() >= cutoff)
				.collect(Collectors.toList());
		long matches = entries.stream().filter(pattern::test).count();
		if (!count.matches(matches)) {
			throwVerificationError(count, matches, pattern, entries, window.getDuration());
		}
	}

	private void throwVerificationError(CountMatchingStrategy count, long actual, FixMessagePattern pattern, List<FixJournalEntry> entries, java.time.Duration window) {
		StringBuilder sb = new StringBuilder();
		sb.append("Verification failed");
		if (window != null) sb.append(" within ").append(window);
		sb.append(": expected ").append(count.describe()).append(" but found ").append(actual).append(" matching entries.\n");
		sb.append("Pattern: ").append(pattern.describe()).append("\n\n");

		if (entries.isEmpty()) {
			sb.append("Journal is empty.");
		} else {
			// Find closest match
			MatchResult bestResult = null;
			FixJournalEntry bestEntry = null;
			double bestScore = -1.0;

			for (FixJournalEntry entry : entries) {
				MatchResult result = pattern.match(entry);
				if (result.getMatchScore() > bestScore) {
					bestScore = result.getMatchScore();
					bestResult = result;
					bestEntry = entry;
				}
			}

			if (bestResult != null && bestScore > 0) {
				sb.append("Closest match found (score ").append(String.format("%.2f", bestScore)).append("):\n");
				sb.append("-------------------------------------------------\n");
				for (MatchResult.FieldMatch fm : bestResult.getFieldMatches()) {
					sb.append(fm.isMatched() ? "[✓] " : "[X] ");
					sb.append(fm.getFieldName()).append(": ");
					if (!fm.isMatched()) {
						sb.append("expected ").append(fm.getExpectedDescription())
								.append(", but was ").append(fm.getActualValue());
					} else {
						sb.append(fm.getActualValue());
					}
					sb.append("\n");
				}
				sb.append("-------------------------------------------------\n");
				sb.append("Full message:\n").append(FixMessagePrettyPrinter.prettyPrint(bestEntry)).append("\n");
			} else {
				sb.append("No partial matches found.\n");
			}

			sb.append("\nRecent messages (up to 5):\n");
			sb.append(String.join("\n---\n", summarizeRecent(5)));
		}

		throw new AssertionError(sb.toString());
	}

	@Override
	public void stubFor(StubMapping stubMapping) {
		stubs.add(stubMapping);
		// Sort by priority (lower number = higher priority)
		stubs.sort(Comparator.comparingInt(StubMapping::getPriority));
	}

	private List<String> summarizeRecent(int max) {
		int from = Math.max(0, journal.size() - max);
		return Collections.unmodifiableList(new ArrayList<>(journal.subList(from, journal.size()))).stream()
				.map(e -> String.format("[%d] %s %s %s->%s 35=%s\n%s",
						e.getTimestampMillis(), e.getDirection(), e.getBeginString(), e.getSenderCompId(), e.getTargetCompId(), e.getMsgType(), 
						FixMessagePrettyPrinter.prettyPrint(e)))
				.collect(Collectors.toList());
	}

	private static class JournalApplication extends MessageCracker implements Application {
		private final List<FixJournalEntry> journal;
		private final List<StubMapping> stubs;
		private final RecordingService recordingService;
		private final FixMockServerOptions options;
		private final Map<String, String> scenarioStates = new ConcurrentHashMap<>();

		private JournalApplication(List<FixJournalEntry> journal, List<StubMapping> stubs, RecordingService recordingService, FixMockServerOptions options) {
			this.journal = journal;
			this.stubs = stubs;
			this.recordingService = recordingService;
			this.options = options;
		}

		@Override
		public void onCreate(SessionID sessionId) {}

		@Override
		public void onLogon(SessionID sessionId) {}

		@Override
		public void onLogout(SessionID sessionId) {}

		@Override
		public void toAdmin(Message message, SessionID sessionId) {
			journalMessage(message, sessionId, Direction.ADMIN, false);
		}

		@Override
		public void fromAdmin(Message message, SessionID sessionId) {
			FixJournalEntry entry = journalMessage(message, sessionId, Direction.ADMIN, true);
			if (entry != null) {
				processMatchingStubs(entry, message, sessionId);
			}
		}

		@Override
		public void toApp(Message message, SessionID sessionId) {
			journalMessage(message, sessionId, Direction.APP, false);
		}

		@Override
		public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectTagValue, UnsupportedMessageType {
			FixJournalEntry entry = journalMessage(message, sessionId, Direction.APP, true);
			if (entry != null) {
				processMatchingStubs(entry, message, sessionId);
			}
		}

		/**
		 * Creates a journal entry for the message and records it if in capture mode.
		 *
		 * @param message   the FIX message
		 * @param sessionId the session ID
		 * @param direction ADMIN or APP
		 * @param isInbound true for incoming messages, false for outgoing
		 * @return the created journal entry, or null if an error occurred
		 */
		private FixJournalEntry journalMessage(Message message, SessionID sessionId, Direction direction, boolean isInbound) {
			try {
				String bs = message.getHeader().isSetField(BeginString.FIELD)
						? message.getHeader().getString(BeginString.FIELD)
						: sessionId.getBeginString();
				String mt = message.getHeader().getString(MsgType.FIELD);
				String s = message.getHeader().getString(SenderCompID.FIELD);
				String t = message.getHeader().getString(TargetCompID.FIELD);
				Map<Integer, String> tags = MessageExtractor.flatten(message);

				FixJournalEntry entry = FixJournalEntry.now(direction, sessionId.toString(), bs, mt, s, t, message.toString(), tags, isInbound);
				journal.add(entry);

				if (options != null && options.getRecordMode() == FixMockServerOptions.RecordMode.CAPTURE) {
					if (isInbound) {
						recordingService.recordInbound(entry, message);
					} else {
						recordingService.recordOutbound(entry, message);
					}
				}
				return entry;
			} catch (Exception e) {
				log.debug("Error journaling message", e);
				return null;
			}
		}

		/**
		 * Finds and executes the first matching stub for the given journal entry.
		 */
		private void processMatchingStubs(FixJournalEntry entry, Message request, SessionID sessionId) {
			for (StubMapping stub : stubs) {
				if (!stub.getRequestPattern().test(entry)) {
					continue;
				}

				// Check scenario state if applicable
				if (stub.isInScenario()) {
					String currentState = scenarioStates.getOrDefault(stub.getScenarioName(), "Started");
					if (stub.getRequiredScenarioState() != null && !stub.getRequiredScenarioState().equals(currentState)) {
						continue;
					}
				}

				try {
					Message response = stub.getResponse().generateResponse(request);
					if (response != null) {
						if (stub.getResponse().getDelayMillis() > 0) {
							Thread.sleep(stub.getResponse().getDelayMillis());
						}
						Session.sendToTarget(response, sessionId);
					}

					if (stub.isInScenario() && stub.getNewScenarioState() != null) {
						scenarioStates.put(stub.getScenarioName(), stub.getNewScenarioState());
					}

					if (stub.getResponse().shouldDisconnect()) {
						Session.lookupSession(sessionId).disconnect("Stub requested disconnect", false);
					}
				} catch (Exception e) {
					log.error("Error generating or sending stub response", e);
				}
				break; // first matching stub wins
			}
		}
	}

	static class MessageExtractor {
		static Map<Integer, String> flatten(Message message) throws FieldNotFound {
			return messageToMap(message);
		}

		private static Map<Integer, String> messageToMap(Message message) throws FieldNotFound {
			java.util.HashMap<Integer, String> map = new java.util.HashMap<>();
			for (java.util.Iterator<Field<?>> it = message.getHeader().iterator(); it.hasNext();) {
				Field<?> f = it.next();
				map.put(f.getTag(), String.valueOf(f.getObject()));
			}
			for (java.util.Iterator<Field<?>> it = message.iterator(); it.hasNext();) {
				Field<?> f = it.next();
				map.put(f.getTag(), String.valueOf(f.getObject()));
			}
			return map;
		}
	}
}
