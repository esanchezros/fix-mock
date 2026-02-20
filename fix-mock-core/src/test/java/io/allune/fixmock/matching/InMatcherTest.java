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
package io.allune.fixmock.matching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMatcherTest {

	@Test
	void shouldMatchWhenValueInSet() {
		InMatcher matcher = new InMatcher("AAPL", "GOOG", "MSFT");
		assertTrue(matcher.matches("AAPL"));
		assertTrue(matcher.matches("GOOG"));
		assertTrue(matcher.matches("MSFT"));
	}

	@Test
	void shouldRejectWhenValueNotInSet() {
		InMatcher matcher = new InMatcher("AAPL", "GOOG");
		assertFalse(matcher.matches("MSFT"));
	}

	@Test
	void shouldReturnFalseWhenActualIsNull() {
		InMatcher matcher = new InMatcher("AAPL", "GOOG");
		assertFalse(matcher.matches(null));
	}

	@Test
	void shouldReturnCorrectDescription() {
		InMatcher matcher = new InMatcher("AAPL");
		assertEquals("in(AAPL)", matcher.describe());
	}
}
