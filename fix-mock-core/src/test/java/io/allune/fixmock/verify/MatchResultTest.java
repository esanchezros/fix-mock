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

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchResultTest {

	@Test
	void exactMatchShouldReturnScoreOne() {
		MatchResult result = new MatchResult(true, List.of(
				new MatchResult.FieldMatch("MsgType", true, "D", "D")
		));
		assertEquals(1.0, result.getMatchScore());
		assertTrue(result.isExactMatch());
	}

	@Test
	void partialMatchShouldReturnApproximateScore() {
		MatchResult result = new MatchResult(false, List.of(
				new MatchResult.FieldMatch("MsgType", true, "D", "D"),
				new MatchResult.FieldMatch("Symbol", false, "AAPL", "MSFT"),
				new MatchResult.FieldMatch("Side", true, "1", "1")
		));
		assertEquals(2.0 / 3.0, result.getMatchScore(), 0.01);
	}

	@Test
	void noMatchShouldReturnScoreZero() {
		MatchResult result = new MatchResult(false, List.of(
				new MatchResult.FieldMatch("MsgType", false, "D", "8"),
				new MatchResult.FieldMatch("Symbol", false, "AAPL", "MSFT")
		));
		assertEquals(0.0, result.getMatchScore());
	}

	@Test
	void emptyFieldMatchesShouldReturnScoreZero() {
		MatchResult result = new MatchResult(false, Collections.emptyList());
		assertEquals(0.0, result.getMatchScore());
	}

	@Test
	void getFieldMatchesShouldReturnTheList() {
		List<MatchResult.FieldMatch> fields = List.of(
				new MatchResult.FieldMatch("MsgType", true, "D", "D"),
				new MatchResult.FieldMatch("Symbol", false, "AAPL", "MSFT")
		);
		MatchResult result = new MatchResult(false, fields);
		assertEquals(2, result.getFieldMatches().size());
		assertEquals("MsgType", result.getFieldMatches().get(0).getFieldName());
		assertEquals("Symbol", result.getFieldMatches().get(1).getFieldName());
	}

	@Test
	void fieldMatchShouldStoreValuesCorrectly() {
		MatchResult.FieldMatch match = new MatchResult.FieldMatch("Symbol", false, "AAPL", "MSFT");
		assertEquals("Symbol", match.getFieldName());
		assertFalse(match.isMatched());
		assertEquals("AAPL", match.getExpectedDescription());
		assertEquals("MSFT", match.getActualValue());
	}
}
