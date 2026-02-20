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
package io.allune.fixmock.server.http.dto;

import io.allune.fixmock.verify.FixJournalEntry;

/**
 * Lightweight DTO view for a journal entry returned by the REST API.
 */
public class JournalEntryDTO {
	private long timestampMillis;
	private String direction;
	private String sessionId;
	private String beginString;
	private String msgType;
	private String senderCompId;
	private String targetCompId;
	private String raw;

	public static JournalEntryDTO from(FixJournalEntry e) {
		JournalEntryDTO dto = new JournalEntryDTO();
		dto.timestampMillis = e.getTimestampMillis();
		dto.direction = e.getDirection() == null ? null : e.getDirection().name();
		dto.sessionId = e.getSessionId();
		dto.beginString = e.getBeginString();
		dto.msgType = e.getMsgType();
		dto.senderCompId = e.getSenderCompId();
		dto.targetCompId = e.getTargetCompId();
		dto.raw = e.getRaw();
		return dto;
	}

	public long getTimestampMillis() { return timestampMillis; }
	public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }
	public String getDirection() { return direction; }
	public void setDirection(String direction) { this.direction = direction; }
	public String getSessionId() { return sessionId; }
	public void setSessionId(String sessionId) { this.sessionId = sessionId; }
	public String getBeginString() { return beginString; }
	public void setBeginString(String beginString) { this.beginString = beginString; }
	public String getMsgType() { return msgType; }
	public void setMsgType(String msgType) { this.msgType = msgType; }
	public String getSenderCompId() { return senderCompId; }
	public void setSenderCompId(String senderCompId) { this.senderCompId = senderCompId; }
	public String getTargetCompId() { return targetCompId; }
	public void setTargetCompId(String targetCompId) { this.targetCompId = targetCompId; }
	public String getRaw() { return raw; }
	public void setRaw(String raw) { this.raw = raw; }
}
