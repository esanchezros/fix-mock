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
import io.allune.fixmock.verify.MatchResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FixMessagePatternBuilderTest {

	@Test
	void shouldSetDirectionToAppWhenAppIsCalled() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.app();
		assertEquals(Direction.APP, builder.getDirection());
	}

	@Test
	void shouldSetDirectionToAdminWhenAdminIsCalled() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.admin();
		assertEquals(Direction.ADMIN, builder.getDirection());
	}

	@Test
	void shouldLeaveDirectionNullWhenAnyIsCalled() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.any();
		assertNull(builder.getDirection());
	}

	@Test
	void shouldSetMessageType() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.any().messageType("D");
		assertEquals("D", builder.getMsgType());
	}

	@Test
	void shouldSetInboundTrue() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.any().inbound();
		assertTrue(builder.test(createEntry(true)));
	}

	@Test
	void shouldSetOutboundFalse() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.any().outbound();
		assertTrue(builder.test(createEntry(false)));
	}

	@Test
	void shouldReturnTrueForMatchingEntry() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.app()
				.messageType("D");

		FixJournalEntry entry = createEntry(true);
		assertTrue(builder.test(entry));
	}

	@Test
	void shouldReturnFalseForNonMatchingEntry() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.app()
				.messageType("8");

		FixJournalEntry entry = createEntry(true);
		assertFalse(builder.test(entry));
	}

	@Test
	void shouldReturnFalseForNullEntry() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.app()
				.messageType("D");

		assertFalse(builder.test(null));
	}

	@Test
	void shouldAddBodyTagMatcher() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.app()
				.messageType("D")
				.withTag(55, "AAPL");

		FixJournalEntry entry = createEntry(true);
		assertTrue(builder.test(entry));

		assertEquals(1, builder.getTagMatchers().size());
		assertEquals(55, builder.getTagMatchers().get(0).getTag());
	}

	@Test
	void shouldAddHeaderTagMatcher() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.app()
				.messageType("D")
				.withHeader(49, "SENDER");

		FixJournalEntry entry = createEntry(true);
		assertTrue(builder.test(entry));

		assertEquals(1, builder.getHeaderMatchers().size());
		assertEquals(49, builder.getHeaderMatchers().get(0).getTag());
	}

	@Test
	void shouldAddSenderCompIdHeaderMatcher() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.app()
				.messageType("D")
				.senderCompId("SENDER");

		FixJournalEntry entry = createEntry(true);
		assertTrue(builder.test(entry));

		assertEquals(1, builder.getHeaderMatchers().size());
		assertEquals(49, builder.getHeaderMatchers().get(0).getTag());
	}

	@Test
	void shouldAddTargetCompIdHeaderMatcher() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.app()
				.messageType("D")
				.targetCompId("TARGET");

		FixJournalEntry entry = createEntry(true);
		assertTrue(builder.test(entry));

		assertEquals(1, builder.getHeaderMatchers().size());
		assertEquals(56, builder.getHeaderMatchers().get(0).getTag());
	}

	@Test
	void shouldIncludeDirectionAndMsgTypeAndTagMatchersInDescription() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.app()
				.messageType("D")
				.withTag(55, "AAPL");

		String description = builder.describe();
		assertTrue(description.contains("Direction=APP"));
		assertTrue(description.contains("MsgType=D"));
		assertTrue(description.contains("tag[55]"));
	}

	@Test
	void shouldReturnMatchResultWithCorrectFieldDetailsForMatching() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.app()
				.messageType("D")
				.withTag(55, "AAPL");

		FixJournalEntry entry = createEntry(true);
		MatchResult result = builder.match(entry);

		assertTrue(result.isExactMatch());
		assertFalse(result.getFieldMatches().isEmpty());
	}

	@Test
	void shouldReturnMatchResultWithCorrectFieldDetailsForNonMatching() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.app()
				.messageType("8")
				.withTag(55, "GOOG");

		FixJournalEntry entry = createEntry(true);
		MatchResult result = builder.match(entry);

		assertFalse(result.isExactMatch());
		assertTrue(result.getFieldMatches().stream().anyMatch(fm -> !fm.isMatched()));
	}

	@Test
	void shouldReturnNonMatchingResultForNullEntry() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.app()
				.messageType("D");

		MatchResult result = builder.match(null);
		assertFalse(result.isExactMatch());
	}

	@Test
	void shouldSetMsgTypeDForNewOrderSingle() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.app().newOrderSingle();
		assertEquals("D", builder.getMsgType());
	}

	@Test
	void shouldSetMsgType8ForExecutionReport() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.app().executionReport();
		assertEquals("8", builder.getMsgType());
	}

	@Test
	void shouldSetMsgTypeAEForTradeCaptureReport() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.app().tradeCaptureReport();
		assertEquals("AE", builder.getMsgType());
	}

	@Test
	void shouldSetMsgTypeARForTradeCaptureReportAck() {
		FixMessagePatternBuilder builder = FixMessagePatternBuilder.app().tradeCaptureReportAck();
		assertEquals("AR", builder.getMsgType());
	}

	private FixJournalEntry createEntry(boolean inbound) {
		Map<Integer, String> tags = new HashMap<>();
		tags.put(35, "D");
		tags.put(49, "SENDER");
		tags.put(56, "TARGET");
		tags.put(55, "AAPL");
		return new FixJournalEntry(
				System.currentTimeMillis(), Direction.APP, "sess", "FIX.4.4",
				"D", "SENDER", "TARGET", "", tags, inbound);
	}
}
