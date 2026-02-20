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
 * Strategy for matching a single FIX tag value.
 * Implementations provide specific matching logic (equality, regex, range, etc.)
 * and a human-readable description for use in verification error messages.
 *
 * @see io.allune.fixmock.matching.EqualToMatcher
 * @see io.allune.fixmock.matching.RegexMatcher
 * @see io.allune.fixmock.matching.ContainsMatcher
 */
public interface ValueMatcher {

	/**
	 * Tests whether the given tag value matches this matcher's criteria.
	 *
	 * @param value the tag value to test, or {@code null} if the tag is absent
	 * @return {@code true} if the value matches
	 */
	boolean matches(String value);

	/**
	 * Returns a human-readable description of this matcher for error messages.
	 *
	 * @return a descriptive string, e.g. {@code equalTo("AAPL")}
	 */
	String describe();
}
