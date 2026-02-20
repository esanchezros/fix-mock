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

/**
 * Matcher that checks if the actual value contains a substring.
 */
public final class ContainsMatcher implements ValueMatcher {
	private final String substring;

	public ContainsMatcher(String substring) {
		this.substring = substring;
	}

	@Override
	public boolean matches(String actual) {
		return actual != null && actual.contains(substring);
	}

	@Override
	public String describe() {
		return "containing(\"" + substring + "\")";
	}
}
