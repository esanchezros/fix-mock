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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Simple sequence response: each match returns the next response definition's result.
 * After exhausting, it keeps returning the last response.
 */
public final class SequenceResponseDefinition implements ResponseDefinition {
	private final List<ResponseDefinition> responses = new ArrayList<>();
	private int index = 0;

	public SequenceResponseDefinition(List<ResponseDefinition> responses) {
		this.responses.addAll(Objects.requireNonNull(responses));
		if (this.responses.isEmpty()) {
			throw new IllegalArgumentException("SequenceResponseDefinition requires at least one response");
		}
	}

	@Override
	public synchronized Message generateResponse(Message request) throws Exception {
		ResponseDefinition current = responses.get(Math.min(index, responses.size() - 1));
		if (index < responses.size() - 1) index++;
		return current.generateResponse(request);
	}

	@Override
	public synchronized long getDelayMillis() {
		ResponseDefinition current = responses.get(Math.min(index, responses.size() - 1));
		return current.getDelayMillis();
	}

	@Override
	public synchronized boolean shouldDisconnect() {
		ResponseDefinition current = responses.get(Math.min(index, responses.size() - 1));
		return current.shouldDisconnect();
	}

	@Override
	public String describe() {
		return "SequenceResponse[steps=" + responses.size() + "]";
	}
}
