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

class AbsentMatcherTest {

	@Test
	void shouldMatchNull() {
		AbsentMatcher matcher = new AbsentMatcher();
		assertTrue(matcher.matches(null));
	}

	@Test
	void shouldRejectNonNull() {
		AbsentMatcher matcher = new AbsentMatcher();
		assertFalse(matcher.matches("value"));
	}

	@Test
	void shouldRejectEmptyString() {
		AbsentMatcher matcher = new AbsentMatcher();
		assertFalse(matcher.matches(""));
	}

	@Test
	void shouldReturnCorrectDescription() {
		AbsentMatcher matcher = new AbsentMatcher();
		assertEquals("absent()", matcher.describe());
	}
}
