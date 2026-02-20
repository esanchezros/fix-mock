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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Matcher that checks if the actual value is in a set of allowed values.
 */
public final class InMatcher implements ValueMatcher {
	private final Set<String> allowedValues;

	public InMatcher(String... values) {
		this.allowedValues = Arrays.stream(values).collect(Collectors.toSet());
	}

	@Override
	public boolean matches(String actual) {
		return allowedValues.contains(actual);
	}

	@Override
	public String describe() {
		return "in(" + String.join(", ", allowedValues) + ")";
	}
}
