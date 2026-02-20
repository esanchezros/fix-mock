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

import io.allune.fixmock.verify.FixJournalEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.DataDictionary;
import quickfix.DefaultMessageFactory;
import quickfix.Message;
import quickfix.MessageUtils;

import java.io.InputStream;

/** Utility to parse raw FIX strings from journal entries into QuickFIX/J Message instances. */
final class FixMessageParsers {
	private static final Logger log = LoggerFactory.getLogger(FixMessageParsers.class);

	private FixMessageParsers() {}

	static Message parse(FixJournalEntry entry) {
		if (entry == null) return null;
		String raw = entry.getRaw();
		if (raw == null || raw.isEmpty()) return null;
		try {
			DataDictionary dd = tryLoadSessionDictionary(entry.getBeginString());
			return MessageUtils.parse(new DefaultMessageFactory(), dd, raw);
		} catch (Exception e) {
			try {
				// Fallback: no dictionary
				return MessageUtils.parse(new DefaultMessageFactory(), null, raw);
			} catch (Exception ignored) {
				log.debug("Failed to parse FIX message", ignored);
				return null;
			}
		}
	}

	static DataDictionary tryLoadSessionDictionary(String beginString) {
		if (beginString == null) return null;
		String resource = null;
		switch (beginString) {
			case "FIX.4.0": resource = "FIX40.xml"; break;
			case "FIX.4.1": resource = "FIX41.xml"; break;
			case "FIX.4.2": resource = "FIX42.xml"; break;
			case "FIX.4.3": resource = "FIX43.xml"; break;
			case "FIX.4.4": resource = "FIX44.xml"; break;
			case "FIX.5.0": resource = "FIX50.xml"; break;
			case "FIX.5.0SP1": resource = "FIX50SP1.xml"; break;
			case "FIX.5.0SP2": resource = "FIX50SP2.xml"; break;
			case "FIXT.1.1":
				// Transport dictionary alone is insufficient for app messages; return null to let fallback handle.
				// If needed in the future, use both transport and appl dictionaries based on ApplVerID.
				resource = "FIXT11.xml"; // try transport dict at least for header
				break;
			default: resource = null;
		}
		if (resource == null) return null;
		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			if (cl == null) cl = FixMessageParsers.class.getClassLoader();
			InputStream is = cl.getResourceAsStream(resource);
			if (is != null) {
				return new DataDictionary(is);
			}
		} catch (Exception ignored) {
			log.debug("Could not load data dictionary for: {}", beginString, ignored);
		}
		return null;
	}
}
