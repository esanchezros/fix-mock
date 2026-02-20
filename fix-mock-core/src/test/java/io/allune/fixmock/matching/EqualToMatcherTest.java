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

class EqualToMatcherTest {

	@Test
	void shouldMatchExactString() {
		EqualToMatcher matcher = new EqualToMatcher("AAPL");
		assertTrue(matcher.matches("AAPL"));
	}

	@Test
	void shouldRejectDifferentString() {
		EqualToMatcher matcher = new EqualToMatcher("AAPL");
		assertFalse(matcher.matches("GOOG"));
	}

	@Test
	void shouldReturnFalseWhenActualIsNullAndExpectedIsNonNull() {
		EqualToMatcher matcher = new EqualToMatcher("AAPL");
		assertFalse(matcher.matches(null));
	}

	@Test
	void shouldMatchNullActualWhenExpectedIsNull() {
		EqualToMatcher matcher = new EqualToMatcher(null);
		assertTrue(matcher.matches(null));
	}

	@Test
	void shouldHandleEmptyStrings() {
		EqualToMatcher matcher = new EqualToMatcher("");
		assertTrue(matcher.matches(""));
		assertFalse(matcher.matches("something"));
		assertFalse(matcher.matches(null));
	}

	@Test
	void shouldReturnCorrectDescription() {
		EqualToMatcher matcher = new EqualToMatcher("AAPL");
		assertEquals("equalTo(\"AAPL\")", matcher.describe());
	}
}
