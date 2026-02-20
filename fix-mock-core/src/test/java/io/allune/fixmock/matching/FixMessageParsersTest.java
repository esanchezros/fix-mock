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
import quickfix.DataDictionary;
import quickfix.Message;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FixMessageParsersTest {

	@Test
	void shouldReturnNullWhenEntryIsNull() {
		Message result = FixMessageParsers.parse(null);

		assertNull(result);
	}

	@Test
	void shouldReturnNullWhenRawIsNull() {
		FixJournalEntry entry = new FixJournalEntry(
				0, Direction.APP, "sess", "FIX.4.4", "D", "S", "T",
				null, null);

		Message result = FixMessageParsers.parse(entry);

		assertNull(result);
	}

	@Test
	void shouldReturnNullWhenRawIsEmpty() {
		FixJournalEntry entry = new FixJournalEntry(
				0, Direction.APP, "sess", "FIX.4.4", "D", "S", "T",
				"", null);

		Message result = FixMessageParsers.parse(entry);

		assertNull(result);
	}

	@Test
	void shouldParseValidFix44Message() {
		Message msg = new Message();
		msg.getHeader().setString(8, "FIX.4.4");
		msg.getHeader().setString(35, "D");
		msg.getHeader().setString(49, "SENDER");
		msg.getHeader().setString(56, "TARGET");
		msg.setString(55, "AAPL");
		String raw = msg.toString();

		FixJournalEntry entry = new FixJournalEntry(
				0, Direction.APP, "sess", "FIX.4.4", "D", "SENDER", "TARGET",
				raw, null);

		Message parsed = FixMessageParsers.parse(entry);

		assertNotNull(parsed);
	}

	@Test
	void shouldReturnDataDictionaryForFix44() {
		DataDictionary dd = FixMessageParsers.tryLoadSessionDictionary("FIX.4.4");

		assertNotNull(dd);
	}

	@Test
	void shouldReturnDataDictionaryForFix42() {
		DataDictionary dd = FixMessageParsers.tryLoadSessionDictionary("FIX.4.2");

		assertNotNull(dd);
	}

	@Test
	void shouldReturnNullForUnknownBeginString() {
		DataDictionary dd = FixMessageParsers.tryLoadSessionDictionary("FIX.9.9");

		assertNull(dd);
	}

	@Test
	void shouldReturnNullForNullBeginString() {
		DataDictionary dd = FixMessageParsers.tryLoadSessionDictionary(null);

		assertNull(dd);
	}

	@Test
	void shouldReturnDataDictionaryForFix40() {
		DataDictionary dd = FixMessageParsers.tryLoadSessionDictionary("FIX.4.0");

		assertNotNull(dd);
	}

	@Test
	void shouldReturnDataDictionaryForFix41() {
		DataDictionary dd = FixMessageParsers.tryLoadSessionDictionary("FIX.4.1");

		assertNotNull(dd);
	}

	@Test
	void shouldReturnDataDictionaryForFix43() {
		DataDictionary dd = FixMessageParsers.tryLoadSessionDictionary("FIX.4.3");

		assertNotNull(dd);
	}

	@Test
	void shouldReturnDataDictionaryForFix50() {
		DataDictionary dd = FixMessageParsers.tryLoadSessionDictionary("FIX.5.0");

		assertNotNull(dd);
	}

	@Test
	void shouldReturnDataDictionaryForFix50SP1() {
		DataDictionary dd = FixMessageParsers.tryLoadSessionDictionary("FIX.5.0SP1");

		assertNotNull(dd);
	}

	@Test
	void shouldReturnDataDictionaryForFix50SP2() {
		DataDictionary dd = FixMessageParsers.tryLoadSessionDictionary("FIX.5.0SP2");

		assertNotNull(dd);
	}

	@Test
	void shouldReturnDataDictionaryForFixt11() {
		DataDictionary dd = FixMessageParsers.tryLoadSessionDictionary("FIXT.1.1");

		assertNotNull(dd);
	}

	@Test
	void shouldParseMessageWithUnknownBeginStringViaFallback() {
		// Build a message with a non-standard beginString to trigger fallback parsing
		Message msg = new Message();
		msg.getHeader().setString(8, "FIX.4.4");
		msg.getHeader().setString(35, "D");
		msg.getHeader().setString(49, "SENDER");
		msg.getHeader().setString(56, "TARGET");
		String raw = msg.toString();

		// Use an unrecognized beginString so dictionary lookup returns null
		FixJournalEntry entry = new FixJournalEntry(
				0, Direction.APP, "sess", "UNKNOWN", "D", "SENDER", "TARGET",
				raw, null);

		Message parsed = FixMessageParsers.parse(entry);

		// Should still parse via the no-dictionary fallback
		assertNotNull(parsed);
	}

	@Test
	void shouldReturnNullForCompletelyInvalidRaw() {
		FixJournalEntry entry = new FixJournalEntry(
				0, Direction.APP, "sess", "FIX.4.4", "D", "S", "T",
				"this is not a FIX message", null);

		Message result = FixMessageParsers.parse(entry);

		assertNull(result);
	}

	@Test
	void shouldParseMessageWithTagsInEntry() {
		Message msg = new Message();
		msg.getHeader().setString(8, "FIX.4.4");
		msg.getHeader().setString(35, "D");
		msg.getHeader().setString(49, "SENDER");
		msg.getHeader().setString(56, "TARGET");
		msg.setString(55, "MSFT");
		String raw = msg.toString();

		Map<Integer, String> tags = new HashMap<>();
		tags.put(35, "D");
		tags.put(55, "MSFT");

		FixJournalEntry entry = new FixJournalEntry(
				System.currentTimeMillis(), Direction.APP, "sess", "FIX.4.4", "D",
				"SENDER", "TARGET", raw, tags);

		Message parsed = FixMessageParsers.parse(entry);

		assertNotNull(parsed);
	}
}
