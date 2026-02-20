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

/**
 * Describes a predicate over FIX messages for verification and stubbing.
 *
 * Implementations will encapsulate matchers for header/body/trailer fields,
 * message direction (admin/app), and optional session constraints.
 */
public interface FixMessagePattern {

	/**
	 * Evaluate whether a journal entry matches this pattern.
	 */
	boolean test(FixJournalEntry entry);

	/**
	 * Evaluate matching details for a journal entry.
	 */
	default MatchResult match(FixJournalEntry entry) {
		return new MatchResult(test(entry), java.util.Collections.emptyList());
	}

	/**
	 * Human-readable description of the pattern (useful in error messages).
	 */
	default String describe() { return getClass().getSimpleName(); }
}
