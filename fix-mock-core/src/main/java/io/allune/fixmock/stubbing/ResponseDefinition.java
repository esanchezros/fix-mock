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
 * Defines how to respond to a matched request.
 * Supports static messages, templated responses, sequences, delays, and connection actions.
 */
public interface ResponseDefinition {

	/**
	 * Generate a FIX message response given the incoming request.
	 * @param request the incoming FIX message that matched the stub
	 * @return the response message to send, or null to send nothing
	 */
	Message generateResponse(Message request) throws Exception;

	/**
	 * Delay in milliseconds before sending the response (0 = no delay).
	 */
	default long getDelayMillis() {
		return 0;
	}

	/**
	 * If true, disconnect the session after sending the response.
	 */
	default boolean shouldDisconnect() {
		return false;
	}

	/**
	 * Human-readable description for debugging.
	 */
	default String describe() {
		return getClass().getSimpleName();
	}
}
