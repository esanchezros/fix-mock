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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CountMatchingStrategyTest {

	// ── atMost ──────────────────────────────────────────────────────────

	@Test
	void atMostDescribeShouldReturnCorrectFormat() {
		CountMatchingStrategy strategy = CountMatchingStrategy.atMost(3);
		assertEquals("atMost(3)", strategy.describe());
	}

	@Test
	void atMostShouldMatchCountBelowLimit() {
		CountMatchingStrategy strategy = CountMatchingStrategy.atMost(3);
		assertTrue(strategy.matches(0));
		assertTrue(strategy.matches(1));
		assertTrue(strategy.matches(2));
	}

	@Test
	void atMostShouldMatchCountEqualToLimit() {
		CountMatchingStrategy strategy = CountMatchingStrategy.atMost(3);
		assertTrue(strategy.matches(3));
	}

	@Test
	void atMostShouldRejectCountAboveLimit() {
		CountMatchingStrategy strategy = CountMatchingStrategy.atMost(3);
		assertFalse(strategy.matches(4));
		assertFalse(strategy.matches(100));
	}

	// ── atLeast ─────────────────────────────────────────────────────────

	@Test
	void atLeastDescribeShouldReturnCorrectFormat() {
		CountMatchingStrategy strategy = CountMatchingStrategy.atLeast(2);
		assertEquals("atLeast(2)", strategy.describe());
	}

	@Test
	void atLeastShouldMatchCountEqualToThreshold() {
		CountMatchingStrategy strategy = CountMatchingStrategy.atLeast(2);
		assertTrue(strategy.matches(2));
	}

	@Test
	void atLeastShouldMatchCountAboveThreshold() {
		CountMatchingStrategy strategy = CountMatchingStrategy.atLeast(2);
		assertTrue(strategy.matches(3));
		assertTrue(strategy.matches(100));
	}

	@Test
	void atLeastShouldRejectCountBelowThreshold() {
		CountMatchingStrategy strategy = CountMatchingStrategy.atLeast(2);
		assertFalse(strategy.matches(0));
		assertFalse(strategy.matches(1));
	}

	// ── exactly ─────────────────────────────────────────────────────────

	@Test
	void exactlyDescribeShouldReturnCorrectFormat() {
		CountMatchingStrategy strategy = CountMatchingStrategy.exactly(1);
		assertEquals("exactly(1)", strategy.describe());
	}

	@Test
	void exactlyShouldMatchExactCount() {
		CountMatchingStrategy strategy = CountMatchingStrategy.exactly(1);
		assertTrue(strategy.matches(1));
	}

	@Test
	void exactlyShouldRejectDifferentCount() {
		CountMatchingStrategy strategy = CountMatchingStrategy.exactly(1);
		assertFalse(strategy.matches(0));
		assertFalse(strategy.matches(2));
	}

	// ── moreThanOrExactly (alias for atLeast) ───────────────────────────

	@Test
	void moreThanOrExactlyShouldMatchExactCount() {
		CountMatchingStrategy strategy = CountMatchingStrategy.moreThanOrExactly(2);
		assertTrue(strategy.matches(2));
	}

	@Test
	void moreThanOrExactlyShouldMatchAboveCount() {
		CountMatchingStrategy strategy = CountMatchingStrategy.moreThanOrExactly(2);
		assertTrue(strategy.matches(3));
	}

	@Test
	void moreThanOrExactlyShouldRejectBelowCount() {
		CountMatchingStrategy strategy = CountMatchingStrategy.moreThanOrExactly(2);
		assertFalse(strategy.matches(1));
	}

	@Test
	void moreThanOrExactlyDescribeShouldReturnAtLeastFormat() {
		CountMatchingStrategy strategy = CountMatchingStrategy.moreThanOrExactly(2);
		assertEquals("atLeast(2)", strategy.describe());
	}
}
