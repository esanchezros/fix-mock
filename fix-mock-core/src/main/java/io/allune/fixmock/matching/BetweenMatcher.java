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
 * Matches when the actual value is between the provided inclusive bounds.
 * Uses numeric comparison when possible, otherwise falls back to lexical comparison.
 */
public final class BetweenMatcher implements ValueMatcher {
	private static final Logger log = LoggerFactory.getLogger(BetweenMatcher.class);

	private final String minInclusive;
	private final String maxInclusive;

	public BetweenMatcher(String minInclusive, String maxInclusive) {
		this.minInclusive = minInclusive;
		this.maxInclusive = maxInclusive;
	}

	@Override
	public boolean matches(String actual) {
		if (actual == null) return false;
		BigDecimal a = parseBigDecimal(actual);
		BigDecimal lo = parseBigDecimal(minInclusive);
		BigDecimal hi = parseBigDecimal(maxInclusive);
		if (a != null && lo != null && hi != null) {
			return a.compareTo(lo) >= 0 && a.compareTo(hi) <= 0;
		}
		// Fallback to lexical inclusive range
		return actual.compareTo(minInclusive) >= 0 && actual.compareTo(maxInclusive) <= 0;
	}

	@Override
	public String describe() {
		return "between(" + minInclusive + ", " + maxInclusive + ")";
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
