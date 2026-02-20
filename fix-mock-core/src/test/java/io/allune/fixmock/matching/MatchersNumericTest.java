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

import io.allune.fixmock.verify.ValueMatcher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for numeric/range matchers with numeric and lexical fallbacks.
 */
class MatchersNumericTest {

	@Test
	void greaterThan_numeric_and_lexical() {
		ValueMatcher gt10 = new GreaterThanMatcher("10");
		assertTrue(gt10.matches("11"));
		assertFalse(gt10.matches("10"));
		assertFalse(gt10.matches("9"));

		// Lexical fallback: "b" > "a"
		ValueMatcher gta = new GreaterThanMatcher("a");
		assertTrue(gta.matches("b"));
		assertFalse(gta.matches("a"));
		assertFalse(gta.matches(""));
		assertFalse(gta.matches(null));
	}

	@Test
	void lessThan_numeric_and_lexical() {
		ValueMatcher lt10 = new LessThanMatcher("10");
		assertTrue(lt10.matches("9"));
		assertFalse(lt10.matches("10"));
		assertFalse(lt10.matches("11"));

		// Lexical fallback: "a" < "b"
		ValueMatcher ltb = new LessThanMatcher("b");
		assertTrue(ltb.matches("a"));
		assertFalse(ltb.matches("b"));
		assertFalse(ltb.matches("c"));
		assertFalse(ltb.matches(null));
	}

	@Test
	void between_inclusive_numeric_and_lexical() {
		ValueMatcher between = new BetweenMatcher("10", "20");
		assertTrue(between.matches("10"));
		assertTrue(between.matches("15"));
		assertTrue(between.matches("20"));
		assertFalse(between.matches("9"));
		assertFalse(between.matches("21"));
		assertFalse(between.matches(null));

		// Lexical inclusive range: [aa, az]
		ValueMatcher bx = new BetweenMatcher("aa", "az");
		assertTrue(bx.matches("aa"));
		assertTrue(bx.matches("am"));
		assertTrue(bx.matches("az"));
		assertFalse(bx.matches("a"));
		assertFalse(bx.matches("b"));
	}
}
