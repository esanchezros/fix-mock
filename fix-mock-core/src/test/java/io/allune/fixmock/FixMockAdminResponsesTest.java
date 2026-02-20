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

import io.allune.fixmock.stubbing.ResponseDefinition;
import io.allune.fixmock.stubbing.SequenceResponseDefinition;
import io.allune.fixmock.stubbing.StaticResponseDefinition;
import org.junit.jupiter.api.Test;
import quickfix.Message;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class FixMockAdminResponsesTest {

	@Test
	void shouldCreateLogonResponse() throws Exception {
		ResponseDefinition rd = FixMock.logonResponse();

		assertNotNull(rd);
		assertInstanceOf(StaticResponseDefinition.class, rd);

		Message msg = rd.generateResponse(null);
		assertNotNull(msg);
		assertEquals("A", msg.getHeader().getString(35));
	}

	@Test
	void shouldCreateLogoutResponse() throws Exception {
		ResponseDefinition rd = FixMock.logoutResponse();

		assertNotNull(rd);
		assertInstanceOf(StaticResponseDefinition.class, rd);

		Message msg = rd.generateResponse(null);
		assertNotNull(msg);
		assertEquals("5", msg.getHeader().getString(35));
	}

	@Test
	void shouldCreateHeartbeatResponse() throws Exception {
		ResponseDefinition rd = FixMock.heartbeat();

		assertNotNull(rd);
		assertInstanceOf(StaticResponseDefinition.class, rd);

		Message msg = rd.generateResponse(null);
		assertNotNull(msg);
		assertEquals("0", msg.getHeader().getString(35));
	}

	@Test
	void shouldCreateTestRequestResponseWithTestReqId() throws Exception {
		ResponseDefinition rd = FixMock.testRequestResponse("TEST");

		assertNotNull(rd);

		Message msg = rd.generateResponse(null);
		assertNotNull(msg);
		assertEquals("0", msg.getHeader().getString(35));
		assertEquals("TEST", msg.getString(112));
	}

	@Test
	void shouldCreateTestRequestWithTestReqId() throws Exception {
		ResponseDefinition rd = FixMock.testRequest("TEST");

		assertNotNull(rd);

		Message msg = rd.generateResponse(null);
		assertNotNull(msg);
		assertEquals("1", msg.getHeader().getString(35));
		assertEquals("TEST", msg.getString(112));
	}

	@Test
	void shouldCreateSequenceResetWithNewSeqNoAndGapFillFlag() throws Exception {
		ResponseDefinition rd = FixMock.sequenceReset(10, true);

		assertNotNull(rd);

		Message msg = rd.generateResponse(null);
		assertNotNull(msg);
		assertEquals("4", msg.getHeader().getString(35));
		assertEquals("10", msg.getString(36));
		assertEquals("Y", msg.getString(123));
	}

	@Test
	void shouldCreateResendRequestWithBeginAndEndSeqNo() throws Exception {
		ResponseDefinition rd = FixMock.resendRequest(1, 5);

		assertNotNull(rd);

		Message msg = rd.generateResponse(null);
		assertNotNull(msg);
		assertEquals("2", msg.getHeader().getString(35));
		assertEquals("1", msg.getString(7));
		assertEquals("5", msg.getString(16));
	}

	@Test
	void shouldCreateRespondWithMessageAndDelay() {
		Message msg = new quickfix.fix44.ExecutionReport();
		ResponseDefinition rd = FixMock.respondWith(msg, Duration.ofMillis(200));

		assertNotNull(rd);
		assertInstanceOf(StaticResponseDefinition.class, rd);
		assertEquals(200, rd.getDelayMillis());
	}

	@Test
	void shouldCreateRespondWithMessageAndNullDelay() {
		Message msg = new quickfix.fix44.ExecutionReport();
		ResponseDefinition rd = FixMock.respondWith(msg, null);

		assertNotNull(rd);
		assertEquals(0, rd.getDelayMillis());
	}

	@Test
	void shouldCreateDisconnectAfterStaticResponseDefinition() throws Exception {
		Message msg = new quickfix.fix44.Logon();
		StaticResponseDefinition srd = new StaticResponseDefinition(msg);

		ResponseDefinition rd = FixMock.disconnectAfter(srd);

		assertNotNull(rd);
		assertTrue(rd.shouldDisconnect());
		assertNotNull(rd.generateResponse(null));
	}

	@Test
	void shouldCreateDisconnectAfterResponseTemplateBuilder() {
		FixMock.ResponseTemplateBuilder rtb = FixMock.respondWithTemplate(() -> {
			Message resp = new Message();
			resp.getHeader().setString(35, "8");
			return resp;
		});

		ResponseDefinition rd = FixMock.disconnectAfter(rtb);

		assertNotNull(rd);
		assertTrue(rd.shouldDisconnect());
		assertInstanceOf(FixMock.ResponseTemplateBuilder.class, rd);
	}

	@Test
	void shouldCreateSequenceWithMultipleResponses() throws Exception {
		Message msg1 = new quickfix.fix44.ExecutionReport();
		msg1.setString(55, "AAPL");
		Message msg2 = new quickfix.fix44.ExecutionReport();
		msg2.setString(55, "GOOG");

		ResponseDefinition rd = FixMock.sequence(
				FixMock.respondWith(msg1),
				FixMock.respondWith(msg2)
		);

		assertNotNull(rd);
		assertInstanceOf(SequenceResponseDefinition.class, rd);

		Message first = rd.generateResponse(null);
		assertEquals("AAPL", first.getString(55));

		Message second = rd.generateResponse(null);
		assertEquals("GOOG", second.getString(55));
	}

	@Test
	void shouldCreateDisconnectAfterGenericResponseDefinition() throws Exception {
		ResponseDefinition generic = new ResponseDefinition() {
			@Override
			public Message generateResponse(Message request) {
				Message resp = new Message();
				resp.getHeader().setString(35, "8");
				return resp;
			}

			@Override
			public String describe() {
				return "generic";
			}
		};

		ResponseDefinition rd = FixMock.disconnectAfter(generic);

		assertNotNull(rd);
		assertTrue(rd.shouldDisconnect());
		assertNotNull(rd.generateResponse(null));
		assertTrue(rd.describe().contains("generic"));
		assertTrue(rd.describe().contains("disconnect"));
		assertEquals(0, rd.getDelayMillis());
	}

	@Test
	void shouldCreateSequenceResetWithGapFillFalse() throws Exception {
		ResponseDefinition rd = FixMock.sequenceReset(5, false);

		Message msg = rd.generateResponse(null);
		assertNotNull(msg);
		assertEquals("N", msg.getString(123));
	}

	@Test
	void shouldCreateDisconnectAfterStaticResponseWithDelay() throws Exception {
		Message msg = new quickfix.fix44.Logon();
		StaticResponseDefinition srd = new StaticResponseDefinition(msg, 300, false);

		ResponseDefinition rd = FixMock.disconnectAfter(srd);

		assertTrue(rd.shouldDisconnect());
		assertEquals(300, rd.getDelayMillis());
	}
}
