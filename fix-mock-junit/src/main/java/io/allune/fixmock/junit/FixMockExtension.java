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
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension to manage a FixMockServer lifecycle.
 * Usage:
 *  @RegisterExtension
 *  static FixMockExtension fix = new FixMockExtension(options);
 */
public class FixMockExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {

	private final FixMockServerOptions options;
	public enum Lifecycle { PER_CLASS, PER_TEST }

	private final Lifecycle lifecycle;
	private final java.time.Duration readinessTimeout;
	private FixMockServer server;

	public FixMockExtension(FixMockServerOptions options) {
		this(options, Lifecycle.PER_CLASS, java.time.Duration.ofSeconds(10));
	}

	public FixMockExtension(FixMockServerOptions options, Lifecycle lifecycle) {
		this(options, lifecycle, java.time.Duration.ofSeconds(10));
	}

	public FixMockExtension(FixMockServerOptions options, Lifecycle lifecycle, java.time.Duration readinessTimeout) {
		this.options = java.util.Objects.requireNonNull(options, "options");
		this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
		this.readinessTimeout = java.util.Objects.requireNonNull(readinessTimeout, "readinessTimeout");
	}

	/** For advanced cases, allow injecting a preconstructed server implementation. */
	public FixMockExtension(FixMockServer server) {
		this.options = null;
		this.lifecycle = Lifecycle.PER_CLASS;
		this.readinessTimeout = java.time.Duration.ofSeconds(10);
		this.server = server;
	}

	@Override
	public void beforeAll(ExtensionContext context) {
		if (lifecycle == Lifecycle.PER_CLASS) startServer();
	}

	@Override
	public void afterAll(ExtensionContext context) {
		if (lifecycle == Lifecycle.PER_CLASS) stopServer();
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		if (lifecycle == Lifecycle.PER_TEST) startServer();
	}

	@Override
	public void afterEach(ExtensionContext context) {
		if (lifecycle == Lifecycle.PER_TEST) stopServer();
	}

	private void startServer() {
		if (server == null || lifecycle == Lifecycle.PER_TEST) {
			server = new io.allune.fixmock.server.SimpleFixMockServer(options);
		}
		server.start();
		if (!server.awaitReady(readinessTimeout)) {
			server.stop(true);
			throw new IllegalStateException("FixMock did not become ready within " + readinessTimeout);
		}
	}

	private void stopServer() {
		if (server != null) server.stop();
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Class<?> type = parameterContext.getParameter().getType();
		return type == FixMockServer.class || type == FixMockExtension.class;
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Class<?> type = parameterContext.getParameter().getType();
		if (type == FixMockExtension.class) return this;
		if (type == FixMockServer.class && server != null) return server;
		throw new ParameterResolutionException("FixMock server is not started");
	}

	// Convenience delegates
	public void stubFor(StubMapping mapping) { server.stubFor(mapping); }
	public void verify(CountMatchingStrategy count, FixMessagePattern pattern) { server.verify(count, pattern); }
	public void verify(CountMatchingStrategy count, TimeWindow window, FixMessagePattern pattern) { server.verify(count, window, pattern); }
	public int port() { return server.getPort(); }
	public int port(quickfix.SessionID sessionId) { return server.getPort(sessionId); }
	public boolean awaitLogon(java.time.Duration timeout) { return server.awaitLogon(timeout); }
	public quickfix.Message awaitMessage(FixMessagePattern pattern, java.time.Duration timeout) { return server.awaitMessage(pattern, timeout); }
	public FixMockServer server() { return server; }
}
