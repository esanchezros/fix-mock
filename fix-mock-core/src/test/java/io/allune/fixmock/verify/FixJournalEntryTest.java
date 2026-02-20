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

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FixJournalEntryTest {

	@Test
	void nowFactoryShouldCreateEntryWithCurrentTimestamp() {
		long before = System.currentTimeMillis();
		FixJournalEntry entry = FixJournalEntry.now(
				Direction.APP, "FIX.4.4:CLIENT->BROKER", "FIX.4.4",
				"D", "CLIENT", "BROKER", "8=FIX.4.4|35=D|",
				Map.of(35, "D", 55, "AAPL")
		);
		long after = System.currentTimeMillis();

		assertTrue(entry.getTimestampMillis() >= before);
		assertTrue(entry.getTimestampMillis() <= after);
	}

	@Test
	void nowFactoryWithInboundFlagShouldSetInbound() {
		FixJournalEntry inbound = FixJournalEntry.now(
				Direction.APP, "FIX.4.4:CLIENT->BROKER", "FIX.4.4",
				"D", "CLIENT", "BROKER", "raw",
				Map.of(35, "D"), true
		);
		assertTrue(inbound.isInbound());

		FixJournalEntry outbound = FixJournalEntry.now(
				Direction.APP, "FIX.4.4:CLIENT->BROKER", "FIX.4.4",
				"D", "CLIENT", "BROKER", "raw",
				Map.of(35, "D"), false
		);
		assertFalse(outbound.isInbound());
	}

	@Test
	void constructorShouldStoreAllFieldsCorrectly() {
		Map<Integer, String> tags = Map.of(35, "D", 55, "AAPL");
		FixJournalEntry entry = new FixJournalEntry(
				123456789L, Direction.ADMIN, "FIX.4.4:A->B", "FIX.4.4",
				"A", "SENDER", "TARGET", "rawMsg", tags, false
		);

		assertEquals(123456789L, entry.getTimestampMillis());
		assertEquals(Direction.ADMIN, entry.getDirection());
		assertEquals("FIX.4.4:A->B", entry.getSessionId());
		assertEquals("FIX.4.4", entry.getBeginString());
		assertEquals("A", entry.getMsgType());
		assertEquals("SENDER", entry.getSenderCompId());
		assertEquals("TARGET", entry.getTargetCompId());
		assertEquals("rawMsg", entry.getRaw());
		assertFalse(entry.isInbound());
	}

	@Test
	void getTagValueShouldReturnCorrectValue() {
		FixJournalEntry entry = FixJournalEntry.now(
				Direction.APP, "sess", "FIX.4.4", "D", "S", "T", "raw",
				Map.of(35, "D", 55, "AAPL")
		);
		assertEquals("D", entry.getTagValue(35));
		assertEquals("AAPL", entry.getTagValue(55));
	}

	@Test
	void getTagValueShouldReturnNullForMissingTag() {
		FixJournalEntry entry = FixJournalEntry.now(
				Direction.APP, "sess", "FIX.4.4", "D", "S", "T", "raw",
				Map.of(35, "D")
		);
		assertNull(entry.getTagValue(999));
	}

	@Test
	void isInboundShouldDefaultToTrueForLegacyConstructor() {
		FixJournalEntry entry = new FixJournalEntry(
				100L, Direction.APP, "sess", "FIX.4.4",
				"D", "S", "T", "raw", Map.of(35, "D")
		);
		assertTrue(entry.isInbound());
	}

	@Test
	void tagsShouldBeUnmodifiable() {
		Map<Integer, String> tags = new HashMap<>();
		tags.put(35, "D");
		FixJournalEntry entry = new FixJournalEntry(
				100L, Direction.APP, "sess", "FIX.4.4",
				"D", "S", "T", "raw", tags
		);
		assertThrows(UnsupportedOperationException.class, () -> entry.getTags().put(999, "X"));
	}

	@Test
	void nullTagsMapShouldBecomeEmptyMap() {
		FixJournalEntry entry = new FixJournalEntry(
				100L, Direction.APP, "sess", "FIX.4.4",
				"D", "S", "T", "raw", null
		);
		assertNotNull(entry.getTags());
		assertTrue(entry.getTags().isEmpty());
	}
}
