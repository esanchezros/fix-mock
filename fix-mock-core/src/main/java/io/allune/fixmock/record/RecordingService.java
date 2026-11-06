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
package io.allune.fixmock.record;

import io.allune.fixmock.FixMock;
import io.allune.fixmock.matching.FixMessagePatternBuilder;
import io.allune.fixmock.stubbing.StubMapping;
import io.allune.fixmock.verify.FixJournalEntry;
import quickfix.Field;
import quickfix.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Captures conversation pairs to generate stubs later.
 */
public final class RecordingService {
	private static final Logger log = LoggerFactory.getLogger(RecordingService.class);

	private final List<RecordingPair> pairs = new CopyOnWriteArrayList<>();
	private final Map<String, java.util.concurrent.ConcurrentLinkedDeque<Message>> pendingRequests = new ConcurrentHashMap<>();

	public void recordInbound(FixJournalEntry entry, Message message) {
		// Per-session compute keeps enqueue atomic with outbound dequeue/removal.
		pendingRequests.compute(entry.getSessionId(), (ignored, requests) -> {
			java.util.concurrent.ConcurrentLinkedDeque<Message> queue = requests == null
					? new java.util.concurrent.ConcurrentLinkedDeque<>() : requests;
			queue.addLast(message);
			return queue;
		});
	}

	public void recordOutbound(FixJournalEntry entry, Message message) {
		// Outbound messages pair with the oldest unmatched inbound message in the session.
		java.util.concurrent.atomic.AtomicReference<Message> request = new java.util.concurrent.atomic.AtomicReference<>();
		pendingRequests.computeIfPresent(entry.getSessionId(), (ignored, requests) -> {
			request.set(requests.pollFirst());
			return requests.isEmpty() ? null : requests;
		});
		if (request.get() != null) pairs.add(new RecordingPair(request.get(), message, entry.getSessionId()));
	}

	public List<RecordingPair> getPairs() {
		return Collections.unmodifiableList(new ArrayList<>(pairs));
	}

	public List<StubMapping> getRecordedStubs() {
		return pairs.stream()
				.map(this::toStubMapping)
				.collect(Collectors.toList());
	}

	private StubMapping toStubMapping(RecordingPair pair) {
		Message req = pair.getRequest();
		Message resp = pair.getResponse();

		String msgType = "unknown";
		try { msgType = req.getHeader().getString(35); } catch (Exception ignored) {
			log.debug("Could not extract MsgType from recorded request", ignored);
		}

		// Create a pattern that matches the request's MsgType and maybe some key tags
		FixMessagePatternBuilder pattern = FixMock.messageType(msgType)
				.sessionId(pair.getSessionId());

		// Simple heuristic: match ClOrdID or other common tags if present
		try {
			if (req.isSetField(11)) pattern.withTag(11, req.getString(11));
			if (req.isSetField(55)) pattern.withTag(55, req.getString(55));
		} catch (Exception ignored) {
			log.debug("Could not extract tag for recording", ignored);
		}

		return FixMock.stubFor(pattern).willReturn(resp);
	}

	public void clear() {
		pairs.clear();
		pendingRequests.clear();
	}

	public static final class RecordingPair {
		private final Message request;
		private final Message response;
		private final String sessionId;

		public RecordingPair(Message request, Message response, String sessionId) {
			this.request = request;
			this.response = response;
			this.sessionId = sessionId;
		}

		public Message getRequest() { return request; }
		public Message getResponse() { return response; }
		public String getSessionId() { return sessionId; }
	}
}
