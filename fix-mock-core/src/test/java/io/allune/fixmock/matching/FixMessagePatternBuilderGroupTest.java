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
import quickfix.Group;
import quickfix.Message;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FixMessagePatternBuilderGroupTest {

	@Test
	void shouldMatchGroupWithAnyElementSelector() {
		FixJournalEntry entry = createEntryWithPartyGroup("CLNT", 1, 3);

		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withGroup(453, FixMessagePatternBuilder.GroupElementSelector.anyElement()
						.withTag(448, new EqualToMatcher("CLNT")));

		assertTrue(pattern.test(entry));
	}

	@Test
	void shouldMatchGroupWithElementAtIndex() {
		FixJournalEntry entry = createEntryWithPartyGroup("CLNT", 1, 3);

		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withGroup(453, FixMessagePatternBuilder.GroupElementSelector.elementAt(0)
						.withTag(448, new EqualToMatcher("CLNT")));

		assertTrue(pattern.test(entry));
	}

	@Test
	void shouldReturnFalseWhenElementAtIndexIsOutOfRange() {
		FixJournalEntry entry = createEntryWithPartyGroup("CLNT", 1, 3);

		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withGroup(453, FixMessagePatternBuilder.GroupElementSelector.elementAt(99)
						.withTag(448, new EqualToMatcher("CLNT")));

		assertFalse(pattern.test(entry));
	}

	@Test
	void shouldMatchGroupWithTagNameOnGroupElement() {
		FixJournalEntry entry = createEntryWithPartyGroup("CLNT", 1, 3);

		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withGroup(453, FixMessagePatternBuilder.GroupElementSelector.anyElement()
						.withTagName("PartyID", new EqualToMatcher("CLNT")));

		assertTrue(pattern.test(entry));
	}

	@Test
	void shouldMatchNestedGroupWithinGroupElement() {
		Message msg = new Message();
		msg.getHeader().setString(8, "FIX.4.4");
		msg.getHeader().setString(35, "AE");
		msg.getHeader().setString(49, "SENDER");
		msg.getHeader().setString(56, "TARGET");

		// Create a TrdCapRptSideGrp (552) with a nested Parties (453)
		Group side = new Group(552, 54);
		side.setString(54, "1");

		Group nestedParty = new Group(453, 448);
		nestedParty.setString(448, "FIRM1");
		nestedParty.setInt(447, 1);
		nestedParty.setInt(452, 1);
		side.addGroup(nestedParty);
		msg.addGroup(side);

		Map<Integer, String> tags = new HashMap<>();
		tags.put(35, "AE");
		tags.put(49, "SENDER");
		tags.put(56, "TARGET");

		FixJournalEntry entry = new FixJournalEntry(
				System.currentTimeMillis(), Direction.APP, "sess", "FIX.4.4", "AE",
				"SENDER", "TARGET", msg.toString(), tags, true);

		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("AE")
				.withGroup(552, FixMessagePatternBuilder.GroupElementSelector.anyElement()
						.withTag(54, new EqualToMatcher("1"))
						.withGroup(453, FixMessagePatternBuilder.GroupElementSelector.anyElement()
								.withTag(448, new EqualToMatcher("FIRM1"))));

		assertTrue(pattern.test(entry));
	}

	@Test
	void shouldReturnFalseWhenGroupNotPresentInMessage() {
		Map<Integer, String> tags = new HashMap<>();
		tags.put(35, "D");
		tags.put(49, "SENDER");
		tags.put(56, "TARGET");
		tags.put(55, "AAPL");

		Message msg = new Message();
		msg.getHeader().setString(8, "FIX.4.4");
		msg.getHeader().setString(35, "D");
		msg.getHeader().setString(49, "SENDER");
		msg.getHeader().setString(56, "TARGET");
		msg.setString(55, "AAPL");

		FixJournalEntry entry = new FixJournalEntry(
				System.currentTimeMillis(), Direction.APP, "sess", "FIX.4.4", "D",
				"SENDER", "TARGET", msg.toString(), tags, true);

		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withGroup(453, FixMessagePatternBuilder.GroupElementSelector.anyElement()
						.withTag(448, new EqualToMatcher("CLNT")));

		assertFalse(pattern.test(entry));
	}

	@Test
	void shouldSupportGroupCriteriaByName() {
		FixJournalEntry entry = createEntryWithPartyGroup("CLNT", 1, 3);

		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withGroup("Parties", FixMessagePatternBuilder.GroupElementSelector.anyElement()
						.withTag(448, new EqualToMatcher("CLNT")));

		assertTrue(pattern.test(entry));
	}

	@Test
	void shouldReturnTrueForIsAnyMode() {
		FixMessagePatternBuilder.GroupElementSelector selector =
				FixMessagePatternBuilder.GroupElementSelector.anyElement();

		assertTrue(selector.isAnyMode());
		assertFalse(selector.isIndexMode());
	}

	@Test
	void shouldReturnTrueForIsIndexMode() {
		FixMessagePatternBuilder.GroupElementSelector selector =
				FixMessagePatternBuilder.GroupElementSelector.elementAt(2);

		assertTrue(selector.isIndexMode());
		assertFalse(selector.isAnyMode());
	}

	@Test
	void shouldReturnCorrectIndexForElementAt() {
		FixMessagePatternBuilder.GroupElementSelector selector =
				FixMessagePatternBuilder.GroupElementSelector.elementAt(3);

		assertEquals(3, selector.getIndex());
	}

	@Test
	void shouldReturnNegativeOneIndexForAnyElement() {
		FixMessagePatternBuilder.GroupElementSelector selector =
				FixMessagePatternBuilder.GroupElementSelector.anyElement();

		assertEquals(-1, selector.getIndex());
	}

	@Test
	void shouldReturnFalseWhenAnyElementDoesNotMatchAnyGroup() {
		FixJournalEntry entry = createEntryWithPartyGroup("CLNT", 1, 3);

		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withGroup(453, FixMessagePatternBuilder.GroupElementSelector.anyElement()
						.withTag(448, new EqualToMatcher("NONEXISTENT")));

		assertFalse(pattern.test(entry));
	}

	@Test
	void shouldMatchSecondElementWithElementAtIndex1() {
		Message msg = new Message();
		msg.getHeader().setString(8, "FIX.4.4");
		msg.getHeader().setString(35, "D");
		msg.getHeader().setString(49, "SENDER");
		msg.getHeader().setString(56, "TARGET");
		msg.setString(55, "AAPL");

		Group party1 = new Group(453, 448);
		party1.setString(448, "PARTY_A");
		party1.setInt(447, 1);
		msg.addGroup(party1);

		Group party2 = new Group(453, 448);
		party2.setString(448, "PARTY_B");
		party2.setInt(447, 2);
		msg.addGroup(party2);

		Map<Integer, String> tags = new HashMap<>();
		tags.put(35, "D");
		tags.put(49, "SENDER");
		tags.put(56, "TARGET");
		tags.put(55, "AAPL");

		FixJournalEntry entry = new FixJournalEntry(
				System.currentTimeMillis(), Direction.APP, "sess", "FIX.4.4", "D",
				"SENDER", "TARGET", msg.toString(), tags, true);

		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withGroup(453, FixMessagePatternBuilder.GroupElementSelector.elementAt(1)
						.withTag(448, new EqualToMatcher("PARTY_B")));

		assertTrue(pattern.test(entry));
	}

	@Test
	void shouldReturnFalseWhenElementAtIndex0MatchesWrongValue() {
		FixJournalEntry entry = createEntryWithPartyGroup("CLNT", 1, 3);

		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withGroup(453, FixMessagePatternBuilder.GroupElementSelector.elementAt(0)
						.withTag(448, new EqualToMatcher("WRONG")));

		assertFalse(pattern.test(entry));
	}

	@Test
	void shouldGetElementMatchers() {
		FixMessagePatternBuilder.GroupElementSelector selector =
				FixMessagePatternBuilder.GroupElementSelector.anyElement()
						.withTag(448, new EqualToMatcher("X"))
						.withTag(447, new EqualToMatcher("1"));

		assertEquals(2, selector.getElementMatchers().size());
	}

	@Test
	void shouldReturnGroupCriteriaAccessors() {
		FixMessagePatternBuilder.GroupElementSelector selector =
				FixMessagePatternBuilder.GroupElementSelector.anyElement();

		FixMessagePatternBuilder.GroupCriteria byTag = new FixMessagePatternBuilder.GroupCriteria(453, selector);
		assertEquals(453, byTag.getGroupTag());
		assertNull(byTag.getGroupName());
		assertSame(selector, byTag.getSelector());

		FixMessagePatternBuilder.GroupCriteria byName = new FixMessagePatternBuilder.GroupCriteria("Parties", selector);
		assertEquals(-1, byName.getGroupTag());
		assertEquals("Parties", byName.getGroupName());
		assertSame(selector, byName.getSelector());
	}

	@Test
	void shouldIncludeGroupCriteriaInPatternBuilderAccessor() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withGroup(453, FixMessagePatternBuilder.GroupElementSelector.anyElement());

		assertEquals(1, pattern.getGroupCriteria().size());
	}

	@Test
	void shouldMatchGroupWithExactValueStringShortcut() {
		FixJournalEntry entry = createEntryWithPartyGroup("CLNT", 1, 3);

		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withGroup(453, FixMessagePatternBuilder.GroupElementSelector.anyElement()
						.withTag(448, "CLNT"));

		assertTrue(pattern.test(entry));
	}

	@Test
	void shouldMatchGroupElementWithTagNameStringShortcut() {
		FixJournalEntry entry = createEntryWithPartyGroup("CLNT", 1, 3);

		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withGroup(453, FixMessagePatternBuilder.GroupElementSelector.anyElement()
						.withTagName("PartyID", "CLNT"));

		assertTrue(pattern.test(entry));
	}

	@Test
	void shouldSupportNestedGroupByName() {
		Message msg = new Message();
		msg.getHeader().setString(8, "FIX.4.4");
		msg.getHeader().setString(35, "AE");
		msg.getHeader().setString(49, "SENDER");
		msg.getHeader().setString(56, "TARGET");

		Group side = new Group(552, 54);
		side.setString(54, "1");

		Group nestedParty = new Group(453, 448);
		nestedParty.setString(448, "FIRM1");
		side.addGroup(nestedParty);
		msg.addGroup(side);

		Map<Integer, String> tags = new HashMap<>();
		tags.put(35, "AE");
		tags.put(49, "SENDER");
		tags.put(56, "TARGET");

		FixJournalEntry entry = new FixJournalEntry(
				System.currentTimeMillis(), Direction.APP, "sess", "FIX.4.4", "AE",
				"SENDER", "TARGET", msg.toString(), tags, true);

		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("AE")
				.withGroup(552, FixMessagePatternBuilder.GroupElementSelector.anyElement()
						.withGroup("Parties", FixMessagePatternBuilder.GroupElementSelector.anyElement()
								.withTag(448, new EqualToMatcher("FIRM1"))));

		assertTrue(pattern.test(entry));
	}

	private FixJournalEntry createEntryWithPartyGroup(String partyId, int partyIdSource, int partyRole) {
		Message msg = new Message();
		msg.getHeader().setString(8, "FIX.4.4");
		msg.getHeader().setString(35, "D");
		msg.getHeader().setString(49, "SENDER");
		msg.getHeader().setString(56, "TARGET");
		msg.setString(55, "AAPL");

		Group party = new Group(453, 448);
		party.setString(448, partyId);
		party.setInt(447, partyIdSource);
		party.setInt(452, partyRole);
		msg.addGroup(party);

		Map<Integer, String> tags = new HashMap<>();
		tags.put(35, "D");
		tags.put(49, "SENDER");
		tags.put(56, "TARGET");
		tags.put(55, "AAPL");

		return new FixJournalEntry(
				System.currentTimeMillis(), Direction.APP, "sess", "FIX.4.4", "D",
				"SENDER", "TARGET", msg.toString(), tags, true);
	}
}
