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
package io.allune.fixmock.verify;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal journal entry describing a received FIX message.
 * The immutable metadata is safe to share; getMessage() returns a defensive QuickFIX/J value.
 */
public final class FixJournalEntry {
	private final long timestampMillis;
	private final Direction direction;
	private final String sessionId; // human-readable, e.g. FIX.4.4:CLIENT->BROKER
	private final String beginString; // 8=
	private final String msgType;	 // 35=
	private final String senderCompId; // 49=
	private final String targetCompId; // 56=
	private final String raw;		 // full raw FIX string (SOH-separated)
	private final Map<Integer, String> tags; // includes header/body/trailer flattened as strings
	private final boolean inbound;

	public FixJournalEntry(
			long timestampMillis,
			Direction direction,
			String sessionId,
			String beginString,
			String msgType,
			String senderCompId,
			String targetCompId,
			String raw,
			Map<Integer, String> tags) {
		this(timestampMillis, direction, sessionId, beginString, msgType, senderCompId, targetCompId, raw, tags, true);
	}

	public FixJournalEntry(
			long timestampMillis,
			Direction direction,
			String sessionId,
			String beginString,
			String msgType,
			String senderCompId,
			String targetCompId,
			String raw,
			Map<Integer, String> tags,
			boolean inbound) {
		this.timestampMillis = timestampMillis;
		this.direction = direction;
		this.sessionId = sessionId;
		this.beginString = beginString;
		this.msgType = msgType;
		this.senderCompId = senderCompId;
		this.targetCompId = targetCompId;
		this.raw = raw;
		this.tags = tags == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(tags));
		this.inbound = inbound;
	}

	public static FixJournalEntry now(
			Direction direction,
			String sessionId,
			String beginString,
			String msgType,
			String senderCompId,
			String targetCompId,
			String raw,
			Map<Integer, String> tags) {
		return new FixJournalEntry(Instant.now().toEpochMilli(), direction, sessionId, beginString, msgType, senderCompId, targetCompId, raw, tags, true);
	}

	public static FixJournalEntry now(
			Direction direction,
			String sessionId,
			String beginString,
			String msgType,
			String senderCompId,
			String targetCompId,
			String raw,
			Map<Integer, String> tags,
			boolean inbound) {
		return new FixJournalEntry(Instant.now().toEpochMilli(), direction, sessionId, beginString, msgType, senderCompId, targetCompId, raw, tags, inbound);
	}

	public long getTimestampMillis() { return timestampMillis; }
	public Direction getDirection() { return direction; }
	public String getSessionId() { return sessionId; }
	public String getBeginString() { return beginString; }
	public String getMsgType() { return msgType; }
	public String getSenderCompId() { return senderCompId; }
	public String getTargetCompId() { return targetCompId; }
	public String getRaw() { return raw; }
	public Map<Integer, String> getTags() { return tags; }
	/** Returns a defensive QuickFIX/J message parsed from the recorded wire value. */
	public quickfix.Message getMessage() {
		try {
			return new quickfix.Message(raw);
		} catch (quickfix.InvalidMessage e) {
			throw new IllegalStateException("Recorded FIX message is no longer parseable", e);
		}
	}

	/**
	 * Returns whether this message was inbound (received from counterparty) or outbound (sent to counterparty).
	 * Useful for distinguishing request and response messages that share the same MsgType
	 * (e.g., TradeCaptureReport AE used bidirectionally in trade reporting).
	 *
	 * @return {@code true} for messages received from the counterparty, {@code false} for messages sent
	 */
	public boolean isInbound() { return inbound; }

	public String getTagValue(int tag) { return tags.get(tag); }
}