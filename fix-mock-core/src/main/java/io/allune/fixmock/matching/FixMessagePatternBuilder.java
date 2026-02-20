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
import io.allune.fixmock.verify.FixMessagePattern;
import io.allune.fixmock.verify.MatchResult;
import io.allune.fixmock.verify.ValueMatcher;
import quickfix.field.MsgType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fluent builder for creating FixMessagePattern instances.
 * Mirrors WireMock's request pattern builder style.
 */
public final class FixMessagePatternBuilder implements FixMessagePattern {
	private static final Logger log = LoggerFactory.getLogger(FixMessagePatternBuilder.class);

	private Direction direction;
	private String msgType;
	private String sessionId;
	private Boolean inbound; // null = don't filter
	private final List<TagMatcher> headerMatchers = new ArrayList<>();
	private final List<TagMatcher> tagMatchers = new ArrayList<>();
	private final List<GroupCriteria> groupCriteria = new ArrayList<>();

	private FixMessagePatternBuilder() {}

	public static FixMessagePatternBuilder app() {
		FixMessagePatternBuilder builder = new FixMessagePatternBuilder();
		builder.direction = Direction.APP;
		return builder;
	}

	public static FixMessagePatternBuilder admin() {
		FixMessagePatternBuilder builder = new FixMessagePatternBuilder();
		builder.direction = Direction.ADMIN;
		return builder;
	}

	public static FixMessagePatternBuilder any() {
		return new FixMessagePatternBuilder();
	}

	public FixMessagePatternBuilder messageType(String msgType) {
		this.msgType = msgType;
		return this;
	}

	public FixMessagePatternBuilder newOrderSingle() {
		return messageType(MsgType.ORDER_SINGLE);
	}

	public FixMessagePatternBuilder executionReport() {
		return messageType(MsgType.EXECUTION_REPORT);
	}

	public FixMessagePatternBuilder orderCancelRequest() {
		return messageType(MsgType.ORDER_CANCEL_REQUEST);
	}

	public FixMessagePatternBuilder orderCancelReplaceRequest() {
		return messageType(MsgType.ORDER_CANCEL_REPLACE_REQUEST);
	}

	public FixMessagePatternBuilder orderCancelReject() {
		return messageType(MsgType.ORDER_CANCEL_REJECT);
	}

	public FixMessagePatternBuilder marketDataRequest() {
		return messageType(MsgType.MARKET_DATA_REQUEST);
	}

	public FixMessagePatternBuilder marketDataSnapshotFullRefresh() {
		return messageType(MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH);
	}

	public FixMessagePatternBuilder marketDataIncrementalRefresh() {
		return messageType(MsgType.MARKET_DATA_INCREMENTAL_REFRESH);
	}

	public FixMessagePatternBuilder securityListRequest() {
		return messageType(MsgType.SECURITY_LIST_REQUEST);
	}

	public FixMessagePatternBuilder securityList() {
		return messageType(MsgType.SECURITY_LIST);
	}

	public FixMessagePatternBuilder tradeCaptureReport() {
		return messageType(MsgType.TRADE_CAPTURE_REPORT);
	}

	public FixMessagePatternBuilder tradeCaptureReportAck() {
		return messageType(MsgType.TRADE_CAPTURE_REPORT_ACK);
	}

	public FixMessagePatternBuilder logon() {
		return messageType(MsgType.LOGON);
	}

	public FixMessagePatternBuilder logout() {
		return messageType(MsgType.LOGOUT);
	}

	public FixMessagePatternBuilder heartbeat() {
		return messageType(MsgType.HEARTBEAT);
	}

	public FixMessagePatternBuilder testRequest() {
		return messageType(MsgType.TEST_REQUEST);
	}

	public FixMessagePatternBuilder resendRequest() {
		return messageType(MsgType.RESEND_REQUEST);
	}

	public FixMessagePatternBuilder sequenceReset() {
		return messageType(MsgType.SEQUENCE_RESET);
	}

	public FixMessagePatternBuilder sessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	/**
	 * Restricts matching to inbound (received) messages only.
	 * Useful when request and response share the same MsgType (e.g., AE in trade reporting).
	 */
	public FixMessagePatternBuilder inbound() {
		this.inbound = true;
		return this;
	}

	/**
	 * Restricts matching to outbound (sent) messages only.
	 */
	public FixMessagePatternBuilder outbound() {
		this.inbound = false;
		return this;
	}

	public Direction getDirection() { return direction; }
	public String getMsgType() { return msgType; }
	public String getSessionId() { return sessionId; }
	public List<TagMatcher> getHeaderMatchers() { return headerMatchers; }
	public List<TagMatcher> getTagMatchers() { return tagMatchers; }
	public List<GroupCriteria> getGroupCriteria() { return groupCriteria; }

	public FixMessagePatternBuilder withHeader(int tag, ValueMatcher matcher) {
		headerMatchers.add(new TagMatcher(tag, matcher));
		return this;
	}

	public FixMessagePatternBuilder withHeader(int tag, String exactValue) {
		return withHeader(tag, new EqualToMatcher(exactValue));
	}

	public FixMessagePatternBuilder withTag(int tag, ValueMatcher matcher) {
		tagMatchers.add(new TagMatcher(tag, matcher));
		return this;
	}

	public FixMessagePatternBuilder withTag(int tag, String exactValue) {
		return withTag(tag, new EqualToMatcher(exactValue));
	}

	public FixMessagePatternBuilder beginString(String value) {
		return withHeader(8, value);
	}

	public FixMessagePatternBuilder senderCompId(String value) {
		return withHeader(49, value);
	}

	public FixMessagePatternBuilder targetCompId(String value) {
		return withHeader(56, value);
	}

	public FixMessagePatternBuilder applVerId(String value) {
		return withHeader(1128, value);
	}

	/**
	 * Match repeating groups by tag number using a selector (any element or at specific index).
	 * Example: withGroup(453, GroupElementSelector.anyElement().withTag(448, equalTo("CLNT")))
	 */
	public FixMessagePatternBuilder withTagName(String tagName, ValueMatcher matcher) {
		// Tag resolution happens during test() via dictionary or aliases
		tagMatchers.add(new TagMatcher(tagName, matcher));
		return this;
	}

	public FixMessagePatternBuilder withTagName(String tagName, String exactValue) {
		return withTagName(tagName, new EqualToMatcher(exactValue));
	}

	public FixMessagePatternBuilder withHeaderName(String tagName, ValueMatcher matcher) {
		headerMatchers.add(new TagMatcher(tagName, matcher));
		return this;
	}

	public FixMessagePatternBuilder withHeaderName(String tagName, String exactValue) {
		return withHeaderName(tagName, new EqualToMatcher(exactValue));
	}

	public FixMessagePatternBuilder withGroup(int groupTag, GroupElementSelector selector) {
		groupCriteria.add(new GroupCriteria(groupTag, selector));
		return this;
	}

	public FixMessagePatternBuilder withGroup(String groupName, GroupElementSelector selector) {
		groupCriteria.add(new GroupCriteria(groupName, selector));
		return this;
	}

	@Override
	public MatchResult match(FixJournalEntry entry) {
		if (entry == null) return new MatchResult(false, List.of());

		List<MatchResult.FieldMatch> fm = new ArrayList<>();

		// Check direction
		if (direction != null) {
			fm.add(new MatchResult.FieldMatch("Direction", entry.getDirection() == direction,
					direction.name(), entry.getDirection() == null ? "null" : entry.getDirection().name()));
		}

		// Check inbound/outbound
		if (inbound != null) {
			fm.add(new MatchResult.FieldMatch("Inbound", entry.isInbound() == inbound,
					inbound ? "inbound" : "outbound", entry.isInbound() ? "inbound" : "outbound"));
		}

		// Check message type
		if (msgType != null) {
			fm.add(new MatchResult.FieldMatch("MsgType (35)", msgType.equals(entry.getMsgType()),
					msgType, entry.getMsgType()));
		}

		// Check session ID
		if (sessionId != null) {
			fm.add(new MatchResult.FieldMatch("SessionID", sessionId.equals(entry.getSessionId()),
					sessionId, entry.getSessionId()));
		}

		// Check header matchers
		for (TagMatcher tm : headerMatchers) {
			int actualTag = tm.tag;
			String name = tm.tagName;
			if (actualTag == -1 && name != null) {
				actualTag = TagResolver.resolveTagName(name, entry);
			} else if (actualTag != -1) {
				name = TagResolver.getFieldName(actualTag, entry.getBeginString());
			}
			
			String label = name != null ? name : ("Tag " + actualTag);
			if (actualTag == -1) {
				fm.add(new MatchResult.FieldMatch(label, false, tm.matcher.describe(), "Could not resolve tag name"));
				continue;
			}

			String value = entry.getTagValue(actualTag);
			fm.add(new MatchResult.FieldMatch(label, tm.matcher.matches(value), tm.matcher.describe(), value));
		}

		// Check tag matchers
		for (TagMatcher tm : tagMatchers) {
			int actualTag = tm.tag;
			String name = tm.tagName;
			if (actualTag == -1 && name != null) {
				actualTag = TagResolver.resolveTagName(name, entry);
			} else if (actualTag != -1) {
				name = TagResolver.getFieldName(actualTag, entry.getBeginString());
			}

			String label = name != null ? name : ("Tag " + actualTag);
			if (actualTag == -1) {
				fm.add(new MatchResult.FieldMatch(label, false, tm.matcher.describe(), "Could not resolve tag name"));
				continue;
			}

			String value = entry.getTagValue(actualTag);
			fm.add(new MatchResult.FieldMatch(label, tm.matcher.matches(value), tm.matcher.describe(), value));
		}

		// Check group criteria (requires parsing raw FIX message)
		if (!groupCriteria.isEmpty()) {
			quickfix.Message msg = FixMessageParsers.parse(entry);
			if (msg == null) {
				fm.add(new MatchResult.FieldMatch("Repeating Groups", false, "Pattern has group criteria", "Failed to parse message"));
			} else {
				for (GroupCriteria gc : groupCriteria) {
					int actualGroupTag = gc.groupTag;
					String label = gc.groupName != null ? gc.groupName : ("Group " + gc.groupTag);
					if (actualGroupTag == -1 && gc.groupName != null) {
						actualGroupTag = TagResolver.resolveTagName(gc.groupName, msg);
					}
					fm.add(new MatchResult.FieldMatch(label, gc.selector.test(msg, gc.groupTag, gc.groupName),
							"Group matches selector", "Group does not match selector or not found"));
				}
			}
		}

		boolean exact = fm.stream().allMatch(MatchResult.FieldMatch::isMatched);
		return new MatchResult(exact, fm);
	}

	@Override
	public boolean test(FixJournalEntry entry) {
		if (entry == null) return false;

		// Check direction
		if (direction != null && entry.getDirection() != direction) {
			return false;
		}

		// Check inbound/outbound
		if (inbound != null && entry.isInbound() != inbound) {
			return false;
		}

		// Check message type
		if (msgType != null && !msgType.equals(entry.getMsgType())) {
			return false;
		}

		// Check session ID
		if (sessionId != null && !sessionId.equals(entry.getSessionId())) {
			return false;
		}

		// Check header matchers
		for (TagMatcher tm : headerMatchers) {
			int actualTag = tm.tag;
			if (actualTag == -1 && tm.tagName != null) {
				actualTag = TagResolver.resolveTagName(tm.tagName, entry);
			}
			if (actualTag == -1) return false;

			String value = entry.getTagValue(actualTag);
			if (!tm.matcher.matches(value)) {
				return false;
			}
		}

		// Check tag matchers
		for (TagMatcher tm : tagMatchers) {
			int actualTag = tm.tag;
			if (actualTag == -1 && tm.tagName != null) {
				actualTag = TagResolver.resolveTagName(tm.tagName, entry);
			}
			if (actualTag == -1) return false;

			String value = entry.getTagValue(actualTag);
			if (!tm.matcher.matches(value)) {
				return false;
			}
		}

		// Check group criteria (requires parsing raw FIX message)
		if (!groupCriteria.isEmpty()) {
			quickfix.Message msg = FixMessageParsers.parse(entry);
			if (msg == null) return false;
			for (GroupCriteria gc : groupCriteria) {
				if (!gc.selector.test(msg, gc.groupTag, gc.groupName)) return false;
			}
		}

		return true;
	}

	@Override
	public String describe() {
		StringBuilder sb = new StringBuilder();
		if (direction != null) {
			sb.append("Direction=").append(direction).append(", ");
		}
		if (inbound != null) {
			sb.append("Inbound=").append(inbound).append(", ");
		}
		if (msgType != null) {
			sb.append("MsgType=").append(msgType).append(", ");
		}
		if (sessionId != null) {
			sb.append("SessionID=").append(sessionId).append(", ");
		}
		for (TagMatcher tm : headerMatchers) {
			sb.append("header[").append(tm.tag).append("]=").append(tm.matcher.describe()).append(", ");
		}
		for (TagMatcher tm : tagMatchers) {
			sb.append("tag[").append(tm.tag).append("]=").append(tm.matcher.describe()).append(", ");
		}
		if (sb.length() > 2) {
			sb.setLength(sb.length() - 2);
		}
		return sb.toString();
	}

	public static final class TagMatcher {
		private final int tag;
		private final String tagName;
		private final ValueMatcher matcher;

		TagMatcher(int tag, ValueMatcher matcher) {
			this.tag = tag;
			this.tagName = null;
			this.matcher = Objects.requireNonNull(matcher);
		}

		TagMatcher(String tagName, ValueMatcher matcher) {
			this.tag = -1;
			this.tagName = Objects.requireNonNull(tagName);
			this.matcher = Objects.requireNonNull(matcher);
		}

		public int getTag() { return tag; }
		public String getTagName() { return tagName; }
		public ValueMatcher getMatcher() { return matcher; }
	}

	public static final class GroupCriteria {
		private final int groupTag;
		private final String groupName;
		private final GroupElementSelector selector;

		GroupCriteria(int groupTag, GroupElementSelector selector) {
			this.groupTag = groupTag;
			this.groupName = null;
			this.selector = Objects.requireNonNull(selector);
		}

		GroupCriteria(String groupName, GroupElementSelector selector) {
			this.groupTag = -1;
			this.groupName = Objects.requireNonNull(groupName);
			this.selector = Objects.requireNonNull(selector);
		}

		public int getGroupTag() { return groupTag; }
		public String getGroupName() { return groupName; }
		public GroupElementSelector getSelector() { return selector; }
	}

	/** Selector for a repeating group element; supports any-element or a specific index. */
	public static final class GroupElementSelector {
		private enum Mode { ANY, INDEX }
		private final Mode mode;
		private final int index; // valid when mode == INDEX (0-based)
		private final List<TagMatcher> elementMatchers = new ArrayList<>();
		private final List<GroupCriteria> nestedGroupCriteria = new ArrayList<>();

		private GroupElementSelector(Mode mode, int idx) {
			this.mode = mode; this.index = idx;
		}

		public static GroupElementSelector anyElement() { return new GroupElementSelector(Mode.ANY, -1); }
		public static GroupElementSelector elementAt(int index0) { return new GroupElementSelector(Mode.INDEX, index0); }

		public boolean isAnyMode() { return mode == Mode.ANY; }
		public boolean isIndexMode() { return mode == Mode.INDEX; }
		public int getIndex() { return index; }
		public List<TagMatcher> getElementMatchers() { return elementMatchers; }

		public GroupElementSelector withTag(int tag, ValueMatcher matcher) {
			elementMatchers.add(new TagMatcher(tag, matcher));
			return this;
		}

		public GroupElementSelector withTag(int tag, String exactValue) {
			return withTag(tag, new EqualToMatcher(exactValue));
		}

		public GroupElementSelector withTagName(String tagName, ValueMatcher matcher) {
			elementMatchers.add(new TagMatcher(tagName, matcher));
			return this;
		}

		public GroupElementSelector withTagName(String tagName, String exactValue) {
			return withTagName(tagName, new EqualToMatcher(exactValue));
		}

		/**
		 * Adds a nested group criterion within this group element.
		 * Useful for structures like TrdCapRptSideGrp (552) containing nested Parties (453).
		 */
		public GroupElementSelector withGroup(int groupTag, GroupElementSelector selector) {
			nestedGroupCriteria.add(new GroupCriteria(groupTag, selector));
			return this;
		}

		/**
		 * Adds a nested group criterion by group name within this group element.
		 */
		public GroupElementSelector withGroup(String groupName, GroupElementSelector selector) {
			nestedGroupCriteria.add(new GroupCriteria(groupName, selector));
			return this;
		}

		boolean test(quickfix.Message msg, int groupTag, String groupName) {
			int actualGroupTag = groupTag;
			if (actualGroupTag == -1 && groupName != null) {
				actualGroupTag = TagResolver.resolveTagName(groupName, msg);
			}
			if (actualGroupTag == -1) return false;

			List<quickfix.Group> groups = msg.getGroups(actualGroupTag);
			return testGroups(groups, msg);
		}

		boolean testGroups(List<quickfix.Group> groups, quickfix.Message context) {
			if (groups == null || groups.isEmpty()) return false;
			if (mode == Mode.INDEX) {
				if (index < 0 || index >= groups.size()) return false;
				return elementMatches(groups.get(index), context);
			}
			// ANY
			for (quickfix.Group g : groups) {
				if (elementMatches(g, context)) return true;
			}
			return false;
		}

		private boolean elementMatches(quickfix.FieldMap fm, quickfix.Message context) {
			for (TagMatcher tm : elementMatchers) {
				int actualTag = tm.tag;
				if (actualTag == -1 && tm.tagName != null) {
					actualTag = TagResolver.resolveTagName(tm.tagName, context);
				}
				if (actualTag == -1) return false;

				String v = null;
				try {
					if (fm.isSetField(actualTag)) v = fm.getString(actualTag);
				} catch (Exception ignored) {
					log.debug("Field not present in group element", ignored);
				}
				if (!tm.matcher.matches(v)) return false;
			}

			// Check nested groups within this group element
			for (GroupCriteria ngc : nestedGroupCriteria) {
				int nestedTag = ngc.groupTag;
				if (nestedTag == -1 && ngc.groupName != null) {
					nestedTag = TagResolver.resolveTagName(ngc.groupName, context);
				}
				if (nestedTag == -1) return false;
				List<quickfix.Group> nestedGroups = fm.getGroups(nestedTag);
				if (!ngc.selector.testGroups(nestedGroups, context)) return false;
			}
			return true;
		}
	}
}
