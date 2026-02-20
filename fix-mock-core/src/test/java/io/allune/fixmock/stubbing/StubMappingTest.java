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

import io.allune.fixmock.verify.FixJournalEntry;
import io.allune.fixmock.verify.FixMessagePattern;
import org.junit.jupiter.api.Test;
import quickfix.Message;

import static org.junit.jupiter.api.Assertions.*;

class StubMappingTest {

	private final FixMessagePattern dummyPattern = entry -> false;
	private final ResponseDefinition dummyResponse = new StaticResponseDefinition(new Message());

	@Test
	void shouldStoreRequestPatternResponseAndPriority() {
		StubMapping mapping = new StubMapping(dummyPattern, dummyResponse, 3);

		assertSame(dummyPattern, mapping.getRequestPattern());
		assertSame(dummyResponse, mapping.getResponse());
		assertEquals(3, mapping.getPriority());
	}

	@Test
	void defaultPriorityShouldBeFive() {
		StubMapping mapping = new StubMapping(dummyPattern, dummyResponse);
		assertEquals(5, mapping.getPriority());
	}

	@Test
	void isInScenarioShouldReturnFalseWhenNoScenario() {
		StubMapping mapping = new StubMapping(dummyPattern, dummyResponse, 5);
		assertFalse(mapping.isInScenario());
	}

	@Test
	void isInScenarioShouldReturnTrueWhenScenarioIsSet() {
		StubMapping mapping = new StubMapping(
				dummyPattern, dummyResponse, 5,
				"order-flow", "Started", "OrderPlaced"
		);
		assertTrue(mapping.isInScenario());
	}

	@Test
	void scenarioGettersShouldReturnCorrectValues() {
		StubMapping mapping = new StubMapping(
				dummyPattern, dummyResponse, 1,
				"trade-scenario", "Initial", "Executed"
		);

		assertEquals("trade-scenario", mapping.getScenarioName());
		assertEquals("Initial", mapping.getRequiredScenarioState());
		assertEquals("Executed", mapping.getNewScenarioState());
	}
}
