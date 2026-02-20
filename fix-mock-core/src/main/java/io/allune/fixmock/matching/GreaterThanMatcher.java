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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Matches when the actual value is strictly greater than the expected value.
 * Best-effort numeric comparison (BigDecimal). Falls back to lexical comparison if not numeric.
 */
public final class GreaterThanMatcher implements ValueMatcher {
	private static final Logger log = LoggerFactory.getLogger(GreaterThanMatcher.class);

	private final String expected;

	public GreaterThanMatcher(String expected) {
		this.expected = expected;
	}

	@Override
	public boolean matches(String actual) {
		if (actual == null) return false;
		BigDecimal a = parseBigDecimal(actual);
		BigDecimal b = parseBigDecimal(expected);
		if (a != null && b != null) {
			return a.compareTo(b) > 0;
		}
		// Fallback to lexical comparison
		return actual.compareTo(expected) > 0;
	}

	@Override
	public String describe() {
		return "greaterThan(" + expected + ")";
	}

	private static BigDecimal parseBigDecimal(String s) {
		try {
			return new BigDecimal(s);
		} catch (Exception ignored) {
			log.debug("Cannot parse as BigDecimal: {}", s, ignored);
			return null;
		}
	}
}
