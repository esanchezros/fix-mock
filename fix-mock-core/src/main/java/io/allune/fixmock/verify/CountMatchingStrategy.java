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
 * Mirrors WireMock's CountMatchingStrategy in spirit.
 */
public interface CountMatchingStrategy {

	boolean matches(long count);

	String describe();

	static CountMatchingStrategy exactly(int n) {
		return new CountMatchingStrategy() {
			@Override
			public boolean matches(long count) { return count == n; }
			@Override
			public String describe() { return "exactly(" + n + ")"; }
		};
	}

	static CountMatchingStrategy atLeast(int n) {
		return new CountMatchingStrategy() {
			@Override
			public boolean matches(long count) { return count >= n; }
			@Override
			public String describe() { return "atLeast(" + n + ")"; }
		};
	}

	static CountMatchingStrategy atMost(int n) {
		return new CountMatchingStrategy() {
			@Override
			public boolean matches(long count) { return count <= n; }
			@Override
			public String describe() { return "atMost(" + n + ")"; }
		};
	}

	static CountMatchingStrategy moreThanOrExactly(int n) {
		return atLeast(n);
	}
}
