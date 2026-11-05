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
package io.allune.fixmock.record;

import io.allune.fixmock.verify.Direction;
import io.allune.fixmock.verify.FixJournalEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.Message;

import static org.junit.jupiter.api.Assertions.*;

class RecordingServiceTest {

	private RecordingService service;

	@BeforeEach
	void setUp() {
		service = new RecordingService();
	}

	@Test
	void shouldCreatePairWhenInboundFollowedByOutbound() {
		FixJournalEntry inEntry = new FixJournalEntry(
				0, Direction.APP, "sess1", "FIX.4.4", "D", "S", "T", "", null, true);
		FixJournalEntry outEntry = new FixJournalEntry(
				0, Direction.APP, "sess1", "FIX.4.4", "8", "T", "S", "", null, false);
		Message inMsg = new Message();
		inMsg.getHeader().setString(35, "D");
		Message outMsg = new Message();
		outMsg.getHeader().setString(35, "8");

		service.recordInbound(inEntry, inMsg);
		service.recordOutbound(outEntry, outMsg);

		assertEquals(1, service.getPairs().size());
		RecordingService.RecordingPair pair = service.getPairs().get(0);
		assertSame(inMsg, pair.getRequest());
		assertSame(outMsg, pair.getResponse());
		assertEquals("sess1", pair.getSessionId());
	}

	@Test
	void shouldReturnRecordedPairs() {
		FixJournalEntry inEntry = new FixJournalEntry(
				0, Direction.APP, "sess1", "FIX.4.4", "D", "S", "T", "", null, true);
		FixJournalEntry outEntry = new FixJournalEntry(
				0, Direction.APP, "sess1", "FIX.4.4", "8", "T", "S", "", null, false);
		Message inMsg = new Message();
		inMsg.getHeader().setString(35, "D");
		Message outMsg = new Message();
		outMsg.getHeader().setString(35, "8");

		service.recordInbound(inEntry, inMsg);
		service.recordOutbound(outEntry, outMsg);

		assertEquals(1, service.getPairs().size());
	}

	@Test
	void shouldGenerateStubMappings() {
		FixJournalEntry inEntry = new FixJournalEntry(
				0, Direction.APP, "sess1", "FIX.4.4", "D", "S", "T", "", null, true);
		FixJournalEntry outEntry = new FixJournalEntry(
				0, Direction.APP, "sess1", "FIX.4.4", "8", "T", "S", "", null, false);
		Message inMsg = new Message();
		inMsg.getHeader().setString(35, "D");
		Message outMsg = new Message();
		outMsg.getHeader().setString(35, "8");

		service.recordInbound(inEntry, inMsg);
		service.recordOutbound(outEntry, outMsg);

		assertFalse(service.getRecordedStubs().isEmpty());
		assertEquals(1, service.getRecordedStubs().size());
	}

	@Test
	void shouldClearAllPairs() {
		FixJournalEntry inEntry = new FixJournalEntry(
				0, Direction.APP, "sess1", "FIX.4.4", "D", "S", "T", "", null, true);
		FixJournalEntry outEntry = new FixJournalEntry(
				0, Direction.APP, "sess1", "FIX.4.4", "8", "T", "S", "", null, false);
		Message inMsg = new Message();
		inMsg.getHeader().setString(35, "D");
		Message outMsg = new Message();
		outMsg.getHeader().setString(35, "8");

		service.recordInbound(inEntry, inMsg);
		service.recordOutbound(outEntry, outMsg);

		assertEquals(1, service.getPairs().size());

		service.clear();

		assertTrue(service.getPairs().isEmpty());
	}
	@Test
	void shouldPairBurstRequestsInFifoOrder() {
		FixJournalEntry inbound = new FixJournalEntry(0, Direction.APP, "sess1", "FIX.4.4", "D", "S", "T", "", null, true);
		FixJournalEntry outbound = new FixJournalEntry(0, Direction.APP, "sess1", "FIX.4.4", "8", "T", "S", "", null, false);
		Message firstRequest = new Message();
		Message secondRequest = new Message();
		Message firstResponse = new Message();
		Message secondResponse = new Message();

		service.recordInbound(inbound, firstRequest);
		service.recordInbound(inbound, secondRequest);
		service.recordOutbound(outbound, firstResponse);
		service.recordOutbound(outbound, secondResponse);

		assertSame(firstRequest, service.getPairs().get(0).getRequest());
		assertSame(secondRequest, service.getPairs().get(1).getRequest());
	}


	@Test
	void shouldNotCreatePairForUnmatchedOutbound() {
		FixJournalEntry outEntry = new FixJournalEntry(
				0, Direction.APP, "sess1", "FIX.4.4", "8", "T", "S", "", null, false);
		Message outMsg = new Message();
		outMsg.getHeader().setString(35, "8");

		service.recordOutbound(outEntry, outMsg);

		assertTrue(service.getPairs().isEmpty());
	}
}
