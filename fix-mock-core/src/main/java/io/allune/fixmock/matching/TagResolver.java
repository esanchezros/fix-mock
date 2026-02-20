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
import quickfix.DataDictionary;
import quickfix.DefaultMessageFactory;
import quickfix.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility to resolve field/group names to tag numbers.
 *
 * <h2>Resolution Strategy</h2>
 * <p>Tag names are resolved in the following order:</p>
 * <ol>
 *   <li><b>Common aliases</b> - Built-in map of frequently used field names
 *       (e.g., Symbol, ClOrdID, Side, OrderQty, Parties, MDEntries)</li>
 *   <li><b>FIX Data Dictionary</b> - If a dictionary is available for the
 *       session's BeginString (FIX.4.2, FIX.4.4, or FIX.5.0SP2), it is used
 *       to resolve the field name to a tag number</li>
 * </ol>
 *
 * <h2>Supported FIX Versions for Dictionary Lookup</h2>
 * <ul>
 *   <li>FIX.4.2 - uses bundled FIX42.xml</li>
 *   <li>FIX.4.4 - uses bundled FIX44.xml</li>
 *   <li>FIX.5.0SP2 - uses bundled FIX50SP2.xml</li>
 * </ul>
 *
 * <h2>Common Aliases</h2>
 * <p>The following field names are always recognized without dictionary lookup:</p>
 * <ul>
 *   <li>Header: SenderCompID (49), TargetCompID (56), BeginString (8), MsgType (35)</li>
 *   <li>Order: ClOrdID (11), OrigClOrdID (41), Symbol (55), Side (54), OrdType (40),
 *       Price (44), OrderQty (38)</li>
 *   <li>Execution: ExecType (150), OrdStatus (39)</li>
 *   <li>Parties Group: Parties/NoPartyIDs (453), PartyID (448), PartyIDSource (447),
 *       PartyRole (452)</li>
 *   <li>Market Data: MDEntries/NoMDEntries (268), MDEntryType (269), MDEntryPx (270),
 *       MDEntrySize (271)</li>
 * </ul>
 *
 * <p>If a name cannot be resolved, -1 is returned.</p>
 *
 * @see io.allune.fixmock.matching.FixMessagePatternBuilder#withTagName(String, ValueMatcher)
 */
public final class TagResolver {
	private static final Logger log = LoggerFactory.getLogger(TagResolver.class);

	private TagResolver() {}

	private static final Map<String, Integer> commonAliases = new ConcurrentHashMap<>();
	static {
		commonAliases.put("SenderCompID", 49);
		commonAliases.put("TargetCompID", 56);
		commonAliases.put("BeginString", 8);
		commonAliases.put("MsgType", 35);
		commonAliases.put("ClOrdID", 11);
		commonAliases.put("OrigClOrdID", 41);
		commonAliases.put("Symbol", 55);
		commonAliases.put("Side", 54);
		commonAliases.put("OrdType", 40);
		commonAliases.put("Price", 44);
		commonAliases.put("OrderQty", 38);
		commonAliases.put("ExecType", 150);
		commonAliases.put("OrdStatus", 39);
		
		// Groups
		commonAliases.put("Parties", 453);
		commonAliases.put("NoPartyIDs", 453);
		commonAliases.put("PartyID", 448);
		commonAliases.put("PartyIDSource", 447);
		commonAliases.put("PartyRole", 452);
		
		commonAliases.put("MDEntries", 268);
		commonAliases.put("NoMDEntries", 268);
		commonAliases.put("MDEntryType", 269);
		commonAliases.put("MDEntryPx", 270);
		commonAliases.put("MDEntrySize", 271);
	}

	/**
	 * Registers a custom tag name alias for use with {@code withTagName()}.
	 *
	 * @param name the human-readable tag name (e.g., "ReportingObligation")
	 * @param tag  the numeric tag number (e.g., 22030)
	 */
	public static void registerAlias(String name, int tag) {
		commonAliases.put(Objects.requireNonNull(name, "name"), tag);
	}

	/**
	 * Registers multiple custom tag name aliases at once.
	 *
	 * @param aliases map of tag name to tag number
	 */
	public static void registerAliases(Map<String, Integer> aliases) {
		aliases.forEach((name, tag) -> commonAliases.put(
				Objects.requireNonNull(name, "name"), Objects.requireNonNull(tag, "tag")));
	}

	public static String getFieldName(int tag, String beginString) {
		// Try dictionary first for precision
		DataDictionary dd = tryLoadDictionary(beginString);
		if (dd != null) {
			String name = dd.getFieldName(tag);
			if (name != null) return name;
		}
		
		// Reverse lookup common aliases
		for (Map.Entry<String, Integer> entry : commonAliases.entrySet()) {
			if (entry.getValue() == tag) return entry.getKey();
		}
		
		return "Tag " + tag;
	}

	public static int resolveTagName(String name, FixJournalEntry entry) {
		if (name == null) return -1;
		Integer alias = commonAliases.get(name);
		if (alias != null) return alias;

		// Try to load dictionary for the beginString
		DataDictionary dd = tryLoadDictionary(entry.getBeginString());
		if (dd != null) {
			int tag = dd.getFieldTag(name);
			if (tag > 0) return tag;
		}
		return -1;
	}

	public static int resolveTagName(String name, Message message) {
		if (name == null) return -1;
		Integer alias = commonAliases.get(name);
		if (alias != null) return alias;

		// Try to resolve via message's own data dictionary if available
		// QuickFIX/J messages don't always have DD attached.
		// Fallback to beginString lookup
		try {
			String beginString = message.getHeader().getString(8);
			DataDictionary dd = tryLoadDictionary(beginString);
			if (dd != null) {
				int tag = dd.getFieldTag(name);
				if (tag > 0) return tag;
			}
		} catch (Exception ignored) {
			log.debug("Dictionary lookup failed for tag name: {}", name, ignored);
		}

		return -1;
	}

	private static DataDictionary tryLoadDictionary(String beginString) {
		if (beginString == null) return null;
		String resource = switch (beginString) {
			case "FIX.4.2" -> "FIX42.xml";
			case "FIX.4.4" -> "FIX44.xml";
			case "FIX.5.0SP2" -> "FIX50SP2.xml";
			default -> null;
		};
		if (resource == null) return null;
		try {
			InputStream is = TagResolver.class.getClassLoader().getResourceAsStream(resource);
			if (is != null) return new DataDictionary(is);
		} catch (Exception ignored) {
			log.debug("Could not load data dictionary for: {}", beginString, ignored);
		}
		return null;
	}
}
