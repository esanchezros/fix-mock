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

import io.allune.fixmock.verify.FixMessagePattern;

/**
 * Represents a single stub mapping: when an incoming message matches the request pattern,
 * respond according to the response definition.
 * Mirrors WireMock's StubMapping concept.
 */
public final class StubMapping {
	private final FixMessagePattern requestPattern;
	private final ResponseDefinition response;
	private final int priority; // lower number = higher priority (like WireMock)

	private final String scenarioName;
	private final String requiredScenarioState;
	private final String newScenarioState;

	public StubMapping(FixMessagePattern requestPattern, ResponseDefinition response, int priority, String scenarioName, String requiredScenarioState, String newScenarioState) {
		this.requestPattern = requestPattern;
		this.response = response;
		this.priority = priority;
		this.scenarioName = scenarioName;
		this.requiredScenarioState = requiredScenarioState;
		this.newScenarioState = newScenarioState;
	}

	public StubMapping(FixMessagePattern requestPattern, ResponseDefinition response, int priority) {
		this(requestPattern, response, priority, null, null, null);
	}

	public StubMapping(FixMessagePattern requestPattern, ResponseDefinition response) {
		this(requestPattern, response, 5); // default priority
	}

	public FixMessagePattern getRequestPattern() {
		return requestPattern;
	}

	public ResponseDefinition getResponse() {
		return response;
	}

	public int getPriority() {
		return priority;
	}

	public String getScenarioName() {
		return scenarioName;
	}

	public String getRequiredScenarioState() {
		return requiredScenarioState;
	}

	public String getNewScenarioState() {
		return newScenarioState;
	}

	public boolean isInScenario() {
		return scenarioName != null;
	}

	public boolean matches(quickfix.Message message) {
		// Simplified: convert Message to FixJournalEntry-like structure for pattern testing
		// In practice, we'd extract tags and test against requestPattern
		return false; // placeholder
	}
}
