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
package io.allune.fixmock.stubbing;

import quickfix.Message;

/**
 * Returns a fixed, pre-built FIX message as the response.
 */
public final class StaticResponseDefinition implements ResponseDefinition {
	private final Message response;
	private final long delayMillis;
	private final boolean disconnect;

	public StaticResponseDefinition(Message response) {
		this(response, 0, false);
	}

	public StaticResponseDefinition(Message response, long delayMillis, boolean disconnect) {
		this.response = response;
		this.delayMillis = delayMillis;
		this.disconnect = disconnect;
	}

	public Message getResponse() {
		return response;
	}

	@Override
	public Message generateResponse(Message request) {
		return response;
	}

	@Override
	public long getDelayMillis() {
		return delayMillis;
	}

	@Override
	public boolean shouldDisconnect() {
		return disconnect;
	}

	@Override
	public String describe() {
		String msgType = "?";
		try {
			if (response != null && response.getHeader().isSetField(35)) {
				msgType = response.getHeader().getString(35);
			}
		} catch (Exception e) {
			msgType = "?";
		}
		return "StaticResponse[msgType=" + msgType + ", delay=" + delayMillis + "ms, disconnect=" + disconnect + "]";
	}
}
