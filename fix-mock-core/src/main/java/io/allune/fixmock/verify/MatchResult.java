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

import java.util.ArrayList;
import java.util.List;

/**
 * Result of matching a pattern against a journal entry.
 */
public final class MatchResult {
	private final boolean exactMatch;
	private final List<FieldMatch> fieldMatches;

	public MatchResult(boolean exactMatch, List<FieldMatch> fieldMatches) {
		this.exactMatch = exactMatch;
		this.fieldMatches = new ArrayList<>(fieldMatches);
	}

	public boolean isExactMatch() {
		return exactMatch;
	}

	public List<FieldMatch> getFieldMatches() {
		return fieldMatches;
	}

	/** Returns a score (0.0 to 1.0) based on how many fields matched. */
	public double getMatchScore() {
		if (exactMatch) return 1.0;
		if (fieldMatches.isEmpty()) return 0.0;
		long total = fieldMatches.size();
		long matched = fieldMatches.stream().filter(FieldMatch::isMatched).count();
		return (double) matched / total;
	}

	public static final class FieldMatch {
		private final String fieldName; // e.g. "Tag 55 (Symbol)"
		private final boolean matched;
		private final String expectedDescription;
		private final String actualValue;

		public FieldMatch(String fieldName, boolean matched, String expectedDescription, String actualValue) {
			this.fieldName = fieldName;
			this.matched = matched;
			this.expectedDescription = expectedDescription;
			this.actualValue = actualValue;
		}

		public String getFieldName() { return fieldName; }
		public boolean isMatched() { return matched; }
		public String getExpectedDescription() { return expectedDescription; }
		public String getActualValue() { return actualValue; }
	}
}
