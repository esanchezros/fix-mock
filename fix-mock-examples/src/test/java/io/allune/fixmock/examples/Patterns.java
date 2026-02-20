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
package io.allune.fixmock.examples;

import io.allune.fixmock.verify.FixJournalEntry;
import io.allune.fixmock.verify.FixMessagePattern;

import java.util.Objects;

/** Simple pattern helpers for examples/tests. */
public final class Patterns {
	private Patterns() {}

	public static MsgTypePattern messageType(String msgType) {
		return new MsgTypePattern(msgType);
	}

	public static final class MsgTypePattern implements FixMessagePattern {
		private final String msgType;
		private Integer tagEquals;
		private String tagValue;

		MsgTypePattern(String msgType) {
			this.msgType = Objects.requireNonNull(msgType);
		}

		public MsgTypePattern withTagEquals(int tag, String value) {
			this.tagEquals = tag;
			this.tagValue = value;
			return this;
		}

		@Override
		public boolean test(FixJournalEntry entry) {
			if (entry == null) return false;
			if (!msgType.equals(entry.getMsgType())) return false;
			if (tagEquals == null) return true;
			String v = entry.getTagValue(tagEquals);
			return Objects.equals(tagValue, v);
		}

		@Override
		public String describe() {
			if (tagEquals == null) return "MsgType=" + msgType;
			return "MsgType=" + msgType + ", tag[" + tagEquals + "]==" + tagValue;
		}
	}
}
