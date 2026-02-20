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
package io.allune.fixmock.matching;

import io.allune.fixmock.verify.Direction;
import io.allune.fixmock.verify.FixJournalEntry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TagResolverTest {

	@Test
	void shouldResolveSymbolAlias() {
		FixJournalEntry entry = new FixJournalEntry(0, Direction.APP, "sess", null, "D", "S", "T", "", null);
		assertEquals(55, TagResolver.resolveTagName("Symbol", entry));
	}

	@Test
	void shouldResolveClOrdIDAlias() {
		FixJournalEntry entry = new FixJournalEntry(0, Direction.APP, "sess", null, "D", "S", "T", "", null);
		assertEquals(11, TagResolver.resolveTagName("ClOrdID", entry));
	}

	@Test
	void shouldResolveMsgTypeAlias() {
		FixJournalEntry entry = new FixJournalEntry(0, Direction.APP, "sess", null, "D", "S", "T", "", null);
		assertEquals(35, TagResolver.resolveTagName("MsgType", entry));
	}

	@Test
	void shouldResolvePartiesAlias() {
		FixJournalEntry entry = new FixJournalEntry(0, Direction.APP, "sess", null, "D", "S", "T", "", null);
		assertEquals(453, TagResolver.resolveTagName("Parties", entry));
	}

	@Test
	void shouldResolveNoPartyIDsAlias() {
		FixJournalEntry entry = new FixJournalEntry(0, Direction.APP, "sess", null, "D", "S", "T", "", null);
		assertEquals(453, TagResolver.resolveTagName("NoPartyIDs", entry));
	}

	@Test
	void shouldReturnMinusOneForUnknownName() {
		FixJournalEntry entry = new FixJournalEntry(0, Direction.APP, "sess", null, "D", "S", "T", "", null);
		assertEquals(-1, TagResolver.resolveTagName("CompletelyUnknownTag", entry));
	}

	@Test
	void shouldRegisterCustomAlias() {
		TagResolver.registerAlias("ReportingObligation", 22030);
		FixJournalEntry entry = new FixJournalEntry(0, Direction.APP, "sess", null, "D", "S", "T", "", null);
		assertEquals(22030, TagResolver.resolveTagName("ReportingObligation", entry));
	}

	@Test
	void shouldRegisterMultipleCustomAliases() {
		TagResolver.registerAliases(Map.of(
				"CustomTagA", 99001,
				"CustomTagB", 99002
		));
		FixJournalEntry entry = new FixJournalEntry(0, Direction.APP, "sess", null, "D", "S", "T", "", null);
		assertEquals(99001, TagResolver.resolveTagName("CustomTagA", entry));
		assertEquals(99002, TagResolver.resolveTagName("CustomTagB", entry));
	}

	@Test
	void shouldReturnFieldNameForKnownTag() {
		String name = TagResolver.getFieldName(55, null);
		assertEquals("Symbol", name);
	}

	@Test
	void shouldReturnTagNForUnknownTag() {
		String name = TagResolver.getFieldName(99999, null);
		assertEquals("Tag 99999", name);
	}
}
