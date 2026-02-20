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
package io.allune.fixmock;

import io.allune.fixmock.FixMockServerOptions.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionConfigTest {

	@Test
	void shouldBuildWithBasicAcceptorConfig() {
		SessionConfig config = SessionConfig.builder()
				.beginString("FIX.4.4")
				.senderCompId("SENDER")
				.targetCompId("TARGET")
				.port(9876)
				.build();

		assertEquals("FIX.4.4", config.getBeginString());
		assertEquals("SENDER", config.getSenderCompId());
		assertEquals("TARGET", config.getTargetCompId());
		assertEquals(9876, config.getPort());
		assertEquals(Role.ACCEPTOR, config.getRole());
	}

	@Test
	void shouldBuildWithSenderSubIdAndTargetSubId() {
		SessionConfig config = SessionConfig.builder()
				.beginString("FIX.4.4")
				.senderCompId("SENDER")
				.senderSubId("DESK1")
				.targetCompId("TARGET")
				.targetSubId("DESK2")
				.port(9876)
				.build();

		assertEquals("DESK1", config.getSenderSubId());
		assertEquals("DESK2", config.getTargetSubId());
	}

	@Test
	void shouldThrowForMissingBeginString() {
		SessionConfig.Builder builder = SessionConfig.builder()
				.senderCompId("SENDER")
				.targetCompId("TARGET")
				.port(9876);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	void shouldThrowForMissingSenderCompId() {
		SessionConfig.Builder builder = SessionConfig.builder()
				.beginString("FIX.4.4")
				.targetCompId("TARGET")
				.port(9876);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	void shouldThrowForMissingTargetCompId() {
		SessionConfig.Builder builder = SessionConfig.builder()
				.beginString("FIX.4.4")
				.senderCompId("SENDER")
				.port(9876);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	void shouldThrowForMissingPortOnAcceptorRole() {
		SessionConfig.Builder builder = SessionConfig.builder()
				.beginString("FIX.4.4")
				.senderCompId("SENDER")
				.targetCompId("TARGET");

		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test
	void shouldThrowForMissingHostOnInitiatorRole() {
		SessionConfig.Builder builder = SessionConfig.builder()
				.beginString("FIX.4.4")
				.senderCompId("SENDER")
				.targetCompId("TARGET")
				.role(Role.INITIATOR)
				.port(9876);

		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test
	void shouldThrowForMissingPortOnInitiatorRole() {
		SessionConfig.Builder builder = SessionConfig.builder()
				.beginString("FIX.4.4")
				.senderCompId("SENDER")
				.targetCompId("TARGET")
				.role(Role.INITIATOR)
				.host("localhost");

		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test
	void shouldFormatToStringCorrectly() {
		SessionConfig config = SessionConfig.builder()
				.beginString("FIX.4.4")
				.senderCompId("SENDER")
				.targetCompId("TARGET")
				.port(9876)
				.build();

		assertEquals("FIX.4.4:SENDER->TARGET (ACCEPTOR)", config.toString());
	}
}
