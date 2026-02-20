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
import quickfix.Message;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FixMessagePrettyPrinterTest {

	@Test
	void shouldReturnNullStringForNullEntry() {
		assertEquals("null", FixMessagePrettyPrinter.prettyPrint(null));
	}

	@Test
	void shouldReturnFormattedStringContainingMessageBrace() {
		Message msg = new Message();
		msg.getHeader().setString(35, "D");
		msg.getHeader().setString(49, "SENDER");
		msg.setString(55, "AAPL");

		Map<Integer, String> tags = new HashMap<>();
		tags.put(35, "D");
		tags.put(49, "SENDER");
		tags.put(55, "AAPL");

		FixJournalEntry entry = new FixJournalEntry(
				0, Direction.APP, "sess", "FIX.4.4",
				"D", "S", "T", msg.toString(), tags);

		String result = FixMessagePrettyPrinter.prettyPrint(entry);
		assertTrue(result.contains("Message {"));
	}

	@Test
	void shouldPrettyPrintMessageWithoutDictionary() {
		Message msg = new Message();
		msg.getHeader().setString(35, "D");
		msg.getHeader().setString(49, "SENDER");
		msg.setString(55, "AAPL");

		String result = FixMessagePrettyPrinter.prettyPrint(msg, null);
		assertTrue(result.contains("Message {"));
		assertTrue(result.contains("Header {"));
		assertTrue(result.contains("Body {"));
		assertTrue(result.contains("Trailer {"));
	}
}
