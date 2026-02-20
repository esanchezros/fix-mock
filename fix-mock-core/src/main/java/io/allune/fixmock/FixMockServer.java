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
package io.allune.fixmock;

import io.allune.fixmock.stubbing.StubMapping;
import io.allune.fixmock.verify.CountMatchingStrategy;
import io.allune.fixmock.verify.FixMessagePattern;
import io.allune.fixmock.verify.TimeWindow;

/**
 * Public API entry point for running an embedded FixMock server.
 *
 * Intentionally minimal for v0. Mirrors WireMock's shape where possible.
 */
public interface FixMockServer {

	void start();

	void stop();

	default boolean isRunning() {
		return false;
	}

	/**
	 * Register a stub mapping. When an incoming message matches the request pattern,
	 * the server will respond according to the response definition.
	 */
	void stubFor(StubMapping stubMapping);

	/**
	 * Verify that a number of messages matching the pattern were received.
	 */
	void verify(CountMatchingStrategy count, FixMessagePattern pattern);

	/**
	 * Verify with a time window constraint.
	 */
	void verify(CountMatchingStrategy count, TimeWindow window, FixMessagePattern pattern);

	/**
	 * Send a message when the server is running in INITIATOR role.
	 * Default implementation throws; implementations may override.
	 */
	default void send(quickfix.Message message) {
		throw new UnsupportedOperationException("send(Message) is only supported in INITIATOR role");
	}

	/**
	 * Send a message to a specific session when the server is running in INITIATOR role.
	 */
	default void send(quickfix.Message message, quickfix.SessionID sessionId) {
		throw new UnsupportedOperationException("send(Message, SessionID) is only supported in INITIATOR role");
	}

	/**
	 * Snapshot of the in-memory journal. Implementations may return an immutable copy.
	 * Default returns an empty list.
	 */
	default java.util.List<io.allune.fixmock.verify.FixJournalEntry> getJournalSnapshot() {
		return java.util.Collections.emptyList();
	}

	/**
	 * Clear all recorded journal entries.
	 * @return number of entries removed
	 */
	default int clearJournal() { return 0; }

	/**
	 * Clear all registered stubs.
	 * @return number of stubs removed
	 */
	default int clearStubs() { return 0; }

	/**
	 * Retrieve stubs recorded during the current session (if RecordMode.CAPTURE was enabled).
	 */
	default java.util.List<io.allune.fixmock.stubbing.StubMapping> getRecordedStubs() {
		return java.util.Collections.emptyList();
	}

	/**
	 * Obtain a session-scoped view of the server for easier verification.
	 */
	default SessionScopedFixMockServer session(String sessionId) {
		return new SessionScopedFixMockServer(this, sessionId);
	}

	/**
	 * View of the server scoped to a single session.
	 */
	class SessionScopedFixMockServer {
		private final FixMockServer delegate;
		private final String sessionId;

		private SessionScopedFixMockServer(FixMockServer delegate, String sessionId) {
			this.delegate = delegate;
			this.sessionId = sessionId;
		}

		public void verify(CountMatchingStrategy count, FixMessagePattern pattern) {
			delegate.verify(count, applySession(pattern));
		}

		public void verify(CountMatchingStrategy count, TimeWindow window, FixMessagePattern pattern) {
			delegate.verify(count, window, applySession(pattern));
		}

		private FixMessagePattern applySession(FixMessagePattern pattern) {
			if (pattern instanceof io.allune.fixmock.matching.FixMessagePatternBuilder builder) {
				return builder.sessionId(sessionId);
			}
			// Fallback: wrap it? 
			// For now, most patterns are builders.
			return pattern;
		}
	}
}
