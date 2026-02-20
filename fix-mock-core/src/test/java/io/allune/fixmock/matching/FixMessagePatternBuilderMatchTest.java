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

class FixMessagePatternBuilderMatchTest {

	// ── helpers ──────────────────────────────────────────────────────────

	private FixJournalEntry createEntry(Direction direction, String sessionId, boolean inbound) {
		Map<Integer, String> tags = new HashMap<>();
		tags.put(35, "D");
		tags.put(49, "SENDER");
		tags.put(56, "TARGET");
		tags.put(55, "AAPL");
		return new FixJournalEntry(
				System.currentTimeMillis(), direction, sessionId, "FIX.4.4",
				"D", "SENDER", "TARGET", "", tags, inbound);
	}

	private FixJournalEntry defaultEntry() {
		return createEntry(Direction.APP, "sess1", true);
	}

	// ── match() direction mismatch ──────────────────────────────────────

	@Test
	void shouldReportDirectionFieldOnDirectionMismatch() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.admin()
				.messageType("D");

		MatchResult result = pattern.match(defaultEntry());

		assertFalse(result.isExactMatch());
		MatchResult.FieldMatch dirField = result.getFieldMatches().stream()
				.filter(f -> f.getFieldName().equals("Direction"))
				.findFirst()
				.orElseThrow();
		assertFalse(dirField.isMatched());
		assertEquals("ADMIN", dirField.getExpectedDescription());
		assertEquals("APP", dirField.getActualValue());
	}

	// ── match() inbound mismatch ────────────────────────────────────────

	@Test
	void shouldReportInboundFieldOnInboundMismatch() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.outbound();

		MatchResult result = pattern.match(defaultEntry()); // entry is inbound=true

		assertFalse(result.isExactMatch());
		MatchResult.FieldMatch inField = result.getFieldMatches().stream()
				.filter(f -> f.getFieldName().equals("Inbound"))
				.findFirst()
				.orElseThrow();
		assertFalse(inField.isMatched());
		assertEquals("outbound", inField.getExpectedDescription());
		assertEquals("inbound", inField.getActualValue());
	}

	// ── match() sessionId mismatch ──────────────────────────────────────

	@Test
	void shouldReportSessionIdFieldOnSessionIdMismatch() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.sessionId("other-session");

		MatchResult result = pattern.match(defaultEntry());

		assertFalse(result.isExactMatch());
		MatchResult.FieldMatch sessionField = result.getFieldMatches().stream()
				.filter(f -> f.getFieldName().equals("SessionID"))
				.findFirst()
				.orElseThrow();
		assertFalse(sessionField.isMatched());
		assertEquals("other-session", sessionField.getExpectedDescription());
		assertEquals("sess1", sessionField.getActualValue());
	}

	// ── match() with header tagName that resolves ───────────────────────

	@Test
	void shouldMatchHeaderTagNameThatResolves() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withHeaderName("SenderCompID", new EqualToMatcher("SENDER"));

		MatchResult result = pattern.match(defaultEntry());

		assertTrue(result.isExactMatch());
		MatchResult.FieldMatch headerField = result.getFieldMatches().stream()
				.filter(f -> f.getFieldName().equals("SenderCompID"))
				.findFirst()
				.orElseThrow();
		assertTrue(headerField.isMatched());
	}

	// ── match() with header tagName that fails to resolve ───────────────

	@Test
	void shouldReportCouldNotResolveForUnresolvableHeaderTagName() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withHeaderName("NonExistentFieldXYZ123", new EqualToMatcher("value"));

		MatchResult result = pattern.match(defaultEntry());

		assertFalse(result.isExactMatch());
		MatchResult.FieldMatch field = result.getFieldMatches().stream()
				.filter(f -> f.getFieldName().equals("NonExistentFieldXYZ123"))
				.findFirst()
				.orElseThrow();
		assertFalse(field.isMatched());
		assertEquals("Could not resolve tag name", field.getActualValue());
	}

	// ── match() with body tagName that resolves ─────────────────────────

	@Test
	void shouldMatchBodyTagNameThatResolves() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withTagName("Symbol", new EqualToMatcher("AAPL"));

		MatchResult result = pattern.match(defaultEntry());

		assertTrue(result.isExactMatch());
		MatchResult.FieldMatch field = result.getFieldMatches().stream()
				.filter(f -> f.getFieldName().equals("Symbol"))
				.findFirst()
				.orElseThrow();
		assertTrue(field.isMatched());
		assertEquals("AAPL", field.getActualValue());
	}

	// ── match() with body tagName that fails to resolve ─────────────────

	@Test
	void shouldReportCouldNotResolveForUnresolvableBodyTagName() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withTagName("NonExistentFieldXYZ123", new EqualToMatcher("value"));

		MatchResult result = pattern.match(defaultEntry());

		assertFalse(result.isExactMatch());
		MatchResult.FieldMatch field = result.getFieldMatches().stream()
				.filter(f -> f.getFieldName().equals("NonExistentFieldXYZ123"))
				.findFirst()
				.orElseThrow();
		assertFalse(field.isMatched());
		assertEquals("Could not resolve tag name", field.getActualValue());
	}

	// ── withTagName() creates name-based matcher ────────────────────────

	@Test
	void shouldCreateNameBasedTagMatcherViaWithTagName() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.withTagName("Symbol", new EqualToMatcher("AAPL"));

		assertEquals(1, pattern.getTagMatchers().size());
		FixMessagePatternBuilder.TagMatcher tm = pattern.getTagMatchers().get(0);
		assertEquals(-1, tm.getTag());
		assertEquals("Symbol", tm.getTagName());
	}

	// ── withHeaderName() creates header name-based matcher ──────────────

	@Test
	void shouldCreateNameBasedHeaderMatcherViaWithHeaderName() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.withHeaderName("SenderCompID", new EqualToMatcher("SENDER"));

		assertEquals(1, pattern.getHeaderMatchers().size());
		FixMessagePatternBuilder.TagMatcher tm = pattern.getHeaderMatchers().get(0);
		assertEquals(-1, tm.getTag());
		assertEquals("SenderCompID", tm.getTagName());
	}

	// ── describe() includes tag info for header matchers with tagName ───

	@Test
	void shouldIncludeTagMinusOneForHeaderMatchersCreatedByTagName() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.withHeaderName("SenderCompID", new EqualToMatcher("SENDER"));

		String desc = pattern.describe();
		assertTrue(desc.contains("header[-1]"), "Expected header[-1] in: " + desc);
		assertTrue(desc.contains("equalTo(\"SENDER\")"), "Expected matcher description in: " + desc);
	}

	// ── describe() includes tag info for body matchers with tagName ─────

	@Test
	void shouldIncludeTagMinusOneForBodyMatchersCreatedByTagName() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.withTagName("Symbol", new EqualToMatcher("AAPL"));

		String desc = pattern.describe();
		assertTrue(desc.contains("tag[-1]"), "Expected tag[-1] in: " + desc);
		assertTrue(desc.contains("equalTo(\"AAPL\")"), "Expected matcher description in: " + desc);
	}

	// ── test() with header tagName that resolves and matches ────────────

	@Test
	void shouldReturnTrueWhenHeaderTagNameResolvesAndMatches() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withHeaderName("SenderCompID", new EqualToMatcher("SENDER"));

		assertTrue(pattern.test(defaultEntry()));
	}

	// ── test() with body tagName that resolves and matches ──────────────

	@Test
	void shouldReturnTrueWhenBodyTagNameResolvesAndMatches() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withTagName("Symbol", new EqualToMatcher("AAPL"));

		assertTrue(pattern.test(defaultEntry()));
	}

	// ── test() with body tagName that fails to resolve returns false ────

	@Test
	void shouldReturnFalseWhenBodyTagNameCannotBeResolved() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withTagName("NonExistentFieldXYZ123", new EqualToMatcher("value"));

		assertFalse(pattern.test(defaultEntry()));
	}

	// ── test() with header tagName that fails to resolve returns false ──

	@Test
	void shouldReturnFalseWhenHeaderTagNameCannotBeResolved() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withHeaderName("NonExistentFieldXYZ123", new EqualToMatcher("value"));

		assertFalse(pattern.test(defaultEntry()));
	}

	// ── describe() includes sessionId and inbound ───────────────────────

	@Test
	void shouldIncludeSessionIdInDescription() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.sessionId("my-session");

		String desc = pattern.describe();
		assertTrue(desc.contains("SessionID=my-session"), "Expected SessionID in: " + desc);
	}

	@Test
	void shouldIncludeInboundInDescription() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.inbound();

		String desc = pattern.describe();
		assertTrue(desc.contains("Inbound=true"), "Expected Inbound=true in: " + desc);
	}

	@Test
	void shouldIncludeOutboundInDescription() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.outbound();

		String desc = pattern.describe();
		assertTrue(desc.contains("Inbound=false"), "Expected Inbound=false in: " + desc);
	}

	// ── match() with numeric header tag gets field name from dictionary ─

	@Test
	void shouldResolveDictionaryFieldNameForNumericHeaderTag() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withHeader(49, new EqualToMatcher("SENDER"));

		MatchResult result = pattern.match(defaultEntry());

		assertTrue(result.isExactMatch());
		MatchResult.FieldMatch field = result.getFieldMatches().stream()
				.filter(f -> f.getFieldName().equals("SenderCompID"))
				.findFirst()
				.orElseThrow();
		assertTrue(field.isMatched());
	}

	// ── match() with numeric body tag gets field name from dictionary ───

	@Test
	void shouldResolveDictionaryFieldNameForNumericBodyTag() {
		FixMessagePatternBuilder pattern = FixMessagePatternBuilder.app()
				.messageType("D")
				.withTag(55, new EqualToMatcher("AAPL"));

		MatchResult result = pattern.match(defaultEntry());

		assertTrue(result.isExactMatch());
		MatchResult.FieldMatch field = result.getFieldMatches().stream()
				.filter(f -> f.getFieldName().equals("Symbol"))
				.findFirst()
				.orElseThrow();
		assertTrue(field.isMatched());
	}
}
