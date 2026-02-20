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
package io.allune.fixmock.junit;

import io.allune.fixmock.FixMockServer;
import io.allune.fixmock.FixMockServerOptions;
import io.allune.fixmock.stubbing.StubMapping;
import io.allune.fixmock.verify.CountMatchingStrategy;
import io.allune.fixmock.verify.FixMessagePattern;
import io.allune.fixmock.verify.TimeWindow;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension to manage a FixMockServer lifecycle.
 * Usage:
 *  @RegisterExtension
 *  static FixMockExtension fix = new FixMockExtension(options);
 */
public class FixMockExtension implements BeforeAllCallback, AfterAllCallback {

	private final FixMockServerOptions options;
	private FixMockServer server;

	public FixMockExtension(FixMockServerOptions options) {
		this.options = options;
	}

	/** For advanced cases, allow injecting a preconstructed server implementation. */
	public FixMockExtension(FixMockServer server) {
		this.options = null;
		this.server = server;
	}

	@Override
	public void beforeAll(ExtensionContext context) {
		if (server == null) {
			server = new io.allune.fixmock.server.SimpleFixMockServer(options);
		}
		server.start();
	}

	@Override
	public void afterAll(ExtensionContext context) {
		if (server != null) {
			server.stop();
		}
	}

	// Convenience delegates
	public void stubFor(StubMapping mapping) { server.stubFor(mapping); }
	public void verify(CountMatchingStrategy count, FixMessagePattern pattern) { server.verify(count, pattern); }
	public void verify(CountMatchingStrategy count, TimeWindow window, FixMessagePattern pattern) { server.verify(count, window, pattern); }
	public FixMockServer server() { return server; }
}
