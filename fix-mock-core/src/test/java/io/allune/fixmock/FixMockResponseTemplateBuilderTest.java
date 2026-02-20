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
import org.junit.jupiter.api.Test;
import quickfix.Group;
import quickfix.Message;
import quickfix.field.NoPartyIDs;
import quickfix.field.PartyID;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class FixMockResponseTemplateBuilderTest {

	@Test
	void shouldCreateResponseDefinitionFromRespondWithTemplate() {
		ResponseDefinition rd = FixMock.respondWithTemplate(() -> {
			Message resp = new Message();
			resp.getHeader().setString(35, "8");
			return resp;
		});

		assertNotNull(rd);
		assertInstanceOf(FixMock.ResponseTemplateBuilder.class, rd);
	}

	@Test
	void shouldGenerateNewMessageEachTimeFromSupplier() throws Exception {
		ResponseDefinition rd = FixMock.respondWithTemplate(() -> {
			Message resp = new Message();
			resp.getHeader().setString(35, "8");
			return resp;
		});

		Message request = new Message();
		Message response1 = rd.generateResponse(request);
		Message response2 = rd.generateResponse(request);

		assertNotNull(response1);
		assertNotNull(response2);
		assertNotSame(response1, response2);
	}

	@Test
	void shouldCopyBodyTagsFromRequestToResponse() throws Exception {
		ResponseDefinition rd = FixMock.respondWithTemplate(() -> {
			Message resp = new Message();
			resp.getHeader().setString(35, "8");
			return resp;
		}).copyFromRequestTags(55, 11);

		Message request = new Message();
		request.setString(55, "AAPL");
		request.setString(11, "ORD-001");

		Message response = rd.generateResponse(request);

		assertEquals("AAPL", response.getString(55));
		assertEquals("ORD-001", response.getString(11));
	}

	@Test
	void shouldCopyHeaderTagsFromRequestToResponse() throws Exception {
		ResponseDefinition rd = FixMock.respondWithTemplate(() -> {
			Message resp = new Message();
			resp.getHeader().setString(35, "8");
			return resp;
		}).copyFromRequestTags(49);

		Message request = new Message();
		request.getHeader().setString(49, "SENDER");

		Message response = rd.generateResponse(request);

		assertEquals("SENDER", response.getHeader().getString(49));
	}

	@Test
	void shouldCopyRepeatingGroupsFromRequestToResponse() throws Exception {
		ResponseDefinition rd = FixMock.respondWithTemplate(() -> {
			Message resp = new Message();
			resp.getHeader().setString(35, "AE");
			return resp;
		}).copyGroupsFromRequest(453);

		Message request = new Message();
		request.getHeader().setString(35, "AE");
		Group party = new Group(453, 448);
		party.setString(448, "PARTY1");
		party.setInt(447, 1);
		request.addGroup(party);

		Message response = rd.generateResponse(request);

		assertEquals(1, response.getGroups(453).size());
		assertEquals("PARTY1", response.getGroups(453).get(0).getString(448));
	}

	@Test
	void shouldSetDelayWhenDelayIsCalled() {
		FixMock.ResponseTemplateBuilder rd = FixMock.respondWithTemplate(() -> {
			Message resp = new Message();
			resp.getHeader().setString(35, "8");
			return resp;
		}).delay(Duration.ofMillis(500));

		assertEquals(500, rd.getDelayMillis());
	}

	@Test
	void shouldSetDisconnectFlagWhenDisconnectIsCalled() {
		FixMock.ResponseTemplateBuilder rd = FixMock.respondWithTemplate(() -> {
			Message resp = new Message();
			resp.getHeader().setString(35, "8");
			return resp;
		}).disconnect();

		assertTrue(rd.shouldDisconnect());
	}

	@Test
	void shouldIncludeCopyTagsAndCopyGroupsInfoInDescription() {
		FixMock.ResponseTemplateBuilder rd = FixMock.respondWithTemplate(() -> {
			Message resp = new Message();
			resp.getHeader().setString(35, "8");
			return resp;
		}).copyFromRequestTags(55, 11).copyGroupsFromRequest(453);

		String desc = rd.describe();

		assertTrue(desc.contains("copyTags="));
		assertTrue(desc.contains("55"));
		assertTrue(desc.contains("11"));
		assertTrue(desc.contains("copyGroups="));
		assertTrue(desc.contains("453"));
	}

	@Test
	void shouldReturnNullWhenSupplierReturnsNull() throws Exception {
		ResponseDefinition rd = FixMock.respondWithTemplate(() -> null);

		Message request = new Message();
		Message response = rd.generateResponse(request);

		assertNull(response);
	}

	@Test
	void shouldIgnoreMissingTagsGracefullyWhenCopying() throws Exception {
		ResponseDefinition rd = FixMock.respondWithTemplate(() -> {
			Message resp = new Message();
			resp.getHeader().setString(35, "8");
			return resp;
		}).copyFromRequestTags(55, 9999);

		Message request = new Message();
		request.setString(55, "AAPL");
		// Tag 9999 is not set on the request

		Message response = rd.generateResponse(request);

		assertNotNull(response);
		assertEquals("AAPL", response.getString(55));
		assertFalse(response.isSetField(9999));
	}

	@Test
	void shouldHandleNullDelayDuration() {
		FixMock.ResponseTemplateBuilder rd = FixMock.respondWithTemplate(() -> {
			Message resp = new Message();
			resp.getHeader().setString(35, "8");
			return resp;
		}).delay(null);

		assertEquals(0, rd.getDelayMillis());
	}

	@Test
	void shouldDefaultToNoDisconnect() {
		FixMock.ResponseTemplateBuilder rd = FixMock.respondWithTemplate(() -> {
			Message resp = new Message();
			resp.getHeader().setString(35, "8");
			return resp;
		});

		assertFalse(rd.shouldDisconnect());
	}

	@Test
	void shouldDefaultToZeroDelay() {
		FixMock.ResponseTemplateBuilder rd = FixMock.respondWithTemplate(() -> {
			Message resp = new Message();
			resp.getHeader().setString(35, "8");
			return resp;
		});

		assertEquals(0, rd.getDelayMillis());
	}

	@Test
	void shouldCopyMultipleGroupsFromRequest() throws Exception {
		ResponseDefinition rd = FixMock.respondWithTemplate(() -> {
			Message resp = new Message();
			resp.getHeader().setString(35, "AE");
			return resp;
		}).copyGroupsFromRequest(453, 268);

		Message request = new Message();
		request.getHeader().setString(35, "AE");

		Group party = new Group(453, 448);
		party.setString(448, "PARTY1");
		request.addGroup(party);

		Group mdEntry = new Group(268, 269);
		mdEntry.setString(269, "0");
		request.addGroup(mdEntry);

		Message response = rd.generateResponse(request);

		assertEquals(1, response.getGroups(453).size());
		assertEquals(1, response.getGroups(268).size());
	}

	@Test
	void shouldHandleNullTagsArrayInCopyFromRequestTags() throws Exception {
		ResponseDefinition rd = FixMock.respondWithTemplate(() -> {
			Message resp = new Message();
			resp.getHeader().setString(35, "8");
			return resp;
		}).copyFromRequestTags((int[]) null);

		Message request = new Message();
		request.setString(55, "AAPL");

		Message response = rd.generateResponse(request);

		assertNotNull(response);
	}

	@Test
	void shouldHandleNullGroupTagsArrayInCopyGroupsFromRequest() throws Exception {
		ResponseDefinition rd = FixMock.respondWithTemplate(() -> {
			Message resp = new Message();
			resp.getHeader().setString(35, "8");
			return resp;
		}).copyGroupsFromRequest((int[]) null);

		Message request = new Message();

		Message response = rd.generateResponse(request);

		assertNotNull(response);
	}
}
