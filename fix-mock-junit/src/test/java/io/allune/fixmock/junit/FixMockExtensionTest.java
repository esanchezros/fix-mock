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

import io.allune.fixmock.FixMockServerOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FixMockExtensionTest {

	@RegisterExtension
	static FixMockExtension fix = new FixMockExtension(
			FixMockServerOptions.builder()
					.role(FixMockServerOptions.Role.ACCEPTOR)
					.beginString("FIX.4.4")
					.senderCompId("BROKER")
					.targetCompId("CLIENT")
					.build()
	);

	@Test
	void extensionStartsServer() {
		assertTrue(fix.server().isRunning(), "Server should be running under the extension");
		assertTrue(fix.port() > 0, "Selected port should be exposed");
	}
}
