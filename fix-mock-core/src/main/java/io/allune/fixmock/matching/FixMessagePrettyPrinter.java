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
import quickfix.Field;
import quickfix.Message;

import java.util.Iterator;

/**
 * Utility to pretty-print FIX messages using DataDictionary if possible.
 */
public final class FixMessagePrettyPrinter {
	private FixMessagePrettyPrinter() {}

	public static String prettyPrint(FixJournalEntry entry) {
		if (entry == null) return "null";
		Message msg = FixMessageParsers.parse(entry);
		if (msg == null) return entry.getRaw();

		DataDictionary dd = FixMessageParsers.tryLoadSessionDictionary(entry.getBeginString());
		return prettyPrint(msg, dd);
	}

	public static String prettyPrint(Message msg, DataDictionary dd) {
		StringBuilder sb = new StringBuilder();
		sb.append("Message {\n");
		sb.append("  Header {\n");
		printFields(sb, msg.getHeader().iterator(), dd, "	");
		sb.append("  }\n");
		sb.append("  Body {\n");
		printFields(sb, msg.iterator(), dd, "	");
		sb.append("  }\n");
		sb.append("  Trailer {\n");
		printFields(sb, msg.getTrailer().iterator(), dd, "	");
		sb.append("  }\n");
		sb.append("}");
		return sb.toString();
	}

	private static void printFields(StringBuilder sb, Iterator<Field<?>> it, DataDictionary dd, String indent) {
		while (it.hasNext()) {
			Field<?> f = it.next();
			sb.append(indent).append(f.getTag());
			if (dd != null) {
				String name = dd.getFieldName(f.getTag());
				if (name != null) {
					sb.append(" (").append(name).append(")");
				}
			}
			sb.append(" = ").append(f.getObject()).append("\n");
		}
	}
}
