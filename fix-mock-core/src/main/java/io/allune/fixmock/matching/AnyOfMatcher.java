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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Matcher that succeeds if any of the child matchers match (OR logic).
 */
public final class AnyOfMatcher implements ValueMatcher {
	private final List<ValueMatcher> matchers;

	public AnyOfMatcher(ValueMatcher... matchers) {
		this.matchers = Arrays.asList(matchers);
	}

	@Override
	public boolean matches(String actual) {
		return matchers.stream().anyMatch(m -> m.matches(actual));
	}

	@Override
	public String describe() {
		return "anyOf(" + matchers.stream().map(ValueMatcher::describe).collect(Collectors.joining(", ")) + ")";
	}
}
