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

import io.allune.fixmock.FixMockServerOptions.RecordMode;
import io.allune.fixmock.FixMockServerOptions.Role;
import io.allune.fixmock.stubbing.StubMapping;
import io.allune.fixmock.stubbing.StaticResponseDefinition;
import org.junit.jupiter.api.Test;
import quickfix.fix44.ExecutionReport;

import static org.junit.jupiter.api.Assertions.*;

class FixMockServerOptionsTest {

	@Test
	void shouldBuildWithMinimalAcceptorConfig() {
		FixMockServerOptions options = FixMockServerOptions.builder()
				.beginString("FIX.4.4")
				.senderCompId("SENDER")
				.targetCompId("TARGET")
				.acceptorPort(9876)
				.build();

		assertEquals("FIX.4.4", options.getBeginString());
		assertEquals("SENDER", options.getSenderCompId());
		assertEquals("TARGET", options.getTargetCompId());
		assertEquals(9876, options.getAcceptorPort());
		assertEquals(Role.ACCEPTOR, options.getRole());
	}

	@Test
	void shouldBuildWithInitiatorConfig() {
		FixMockServerOptions options = FixMockServerOptions.builder()
				.role(Role.INITIATOR)
				.beginString("FIX.4.4")
				.senderCompId("SENDER")
				.targetCompId("TARGET")
				.initiatorHost("localhost")
				.initiatorPort(9876)
				.build();

		assertEquals(Role.INITIATOR, options.getRole());
		assertEquals("localhost", options.getInitiatorHost());
		assertEquals(9876, options.getInitiatorPort());
	}

	@Test
	void shouldThrowForMissingBeginString() {
		FixMockServerOptions.Builder builder = FixMockServerOptions.builder()
				.senderCompId("SENDER")
				.targetCompId("TARGET")
				.acceptorPort(9876);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	void shouldThrowForMissingSenderCompId() {
		FixMockServerOptions.Builder builder = FixMockServerOptions.builder()
				.beginString("FIX.4.4")
				.targetCompId("TARGET")
				.acceptorPort(9876);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	void shouldThrowForMissingTargetCompId() {
		FixMockServerOptions.Builder builder = FixMockServerOptions.builder()
				.beginString("FIX.4.4")
				.senderCompId("SENDER")
				.acceptorPort(9876);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	void shouldThrowForMissingAcceptorPortOnAcceptorRole() {
		FixMockServerOptions.Builder builder = FixMockServerOptions.builder()
				.beginString("FIX.4.4")
				.senderCompId("SENDER")
				.targetCompId("TARGET");

		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test
	void shouldThrowForMissingInitiatorHostOnInitiatorRole() {
		FixMockServerOptions.Builder builder = FixMockServerOptions.builder()
				.role(Role.INITIATOR)
				.beginString("FIX.4.4")
				.senderCompId("SENDER")
				.targetCompId("TARGET")
				.initiatorPort(9876);

		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test
	void shouldHaveCorrectDefaults() {
		FixMockServerOptions options = FixMockServerOptions.builder()
				.beginString("FIX.4.4")
				.senderCompId("SENDER")
				.targetCompId("TARGET")
				.acceptorPort(9876)
				.build();

		assertEquals(RecordMode.OFF, options.getRecordMode());
		assertEquals(30, options.getHeartBtIntSeconds());
		assertFalse(options.isUseDataDictionary());
		assertTrue(options.isResetOnLogon());
		assertTrue(options.isResetOnDisconnect());
		assertTrue(options.isResetOnLogout());
	}

	@Test
	void shouldCreateCopyViaToBuilderThatCanBeModified() {
		FixMockServerOptions original = FixMockServerOptions.builder()
				.beginString("FIX.4.4")
				.senderCompId("SENDER")
				.targetCompId("TARGET")
				.acceptorPort(9876)
				.build();

		FixMockServerOptions copy = original.toBuilder()
				.heartBtIntSeconds(60)
				.build();

		assertEquals(30, original.getHeartBtIntSeconds());
		assertEquals(60, copy.getHeartBtIntSeconds());
		// Sessions should be preserved in the copy
		assertFalse(copy.getSessions().isEmpty());
	}

	@Test
	void shouldAddExplicitSessions() {
		SessionConfig session1 = SessionConfig.builder()
				.beginString("FIX.4.4")
				.senderCompId("SENDER1")
				.targetCompId("TARGET1")
				.port(9876)
				.build();

		SessionConfig session2 = SessionConfig.builder()
				.beginString("FIX.4.4")
				.senderCompId("SENDER2")
				.targetCompId("TARGET2")
				.port(9877)
				.build();

		FixMockServerOptions options = FixMockServerOptions.builder()
				.addSession(session1)
				.addSession(session2)
				.build();

		assertEquals(2, options.getSessions().size());
		assertEquals("SENDER1", options.getSessions().get(0).getSenderCompId());
		assertEquals("SENDER2", options.getSessions().get(1).getSenderCompId());
	}

	@Test
	void shouldAddStubMappings() {
		StubMapping stub = new StubMapping(
				FixMock.newOrderSingle(),
				new StaticResponseDefinition(new ExecutionReport())
		);

		FixMockServerOptions options = FixMockServerOptions.builder()
				.beginString("FIX.4.4")
				.senderCompId("SENDER")
				.targetCompId("TARGET")
				.acceptorPort(9876)
				.stubMapping(stub)
				.build();

		assertEquals(1, options.getStubs().size());
		assertSame(stub, options.getStubs().get(0));
	}
}
