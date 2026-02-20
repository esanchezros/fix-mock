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
package io.allune.fixmock;

import io.allune.fixmock.stubbing.ResponseDefinition;
import io.allune.fixmock.stubbing.StubMapping;
import io.allune.fixmock.stubbing.StaticResponseDefinition;
import io.allune.fixmock.stubbing.SequenceResponseDefinition;
import io.allune.fixmock.verify.CountMatchingStrategy;
import io.allune.fixmock.verify.FixMessagePattern;
import io.allune.fixmock.verify.TimeWindow;
import io.allune.fixmock.verify.ValueMatcher;
import io.allune.fixmock.matching.EqualToMatcher;
import io.allune.fixmock.matching.RegexMatcher;
import io.allune.fixmock.matching.ContainsMatcher;
import io.allune.fixmock.matching.InMatcher;
import io.allune.fixmock.matching.AbsentMatcher;
import io.allune.fixmock.matching.PresentMatcher;
import io.allune.fixmock.matching.AnyOfMatcher;
import io.allune.fixmock.matching.AllOfMatcher;
import io.allune.fixmock.matching.NotMatcher;
import io.allune.fixmock.matching.FixMessagePatternBuilder;
import io.allune.fixmock.matching.FixMessagePatternBuilder.GroupElementSelector;
import io.allune.fixmock.matching.GreaterThanMatcher;
import io.allune.fixmock.matching.LessThanMatcher;
import io.allune.fixmock.matching.BetweenMatcher;
import quickfix.Message;
import quickfix.field.ExecType;
import quickfix.field.OrdType;
import quickfix.field.Side;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Static DSL entry point for FixMock, mirroring WireMock's fluent API style.
 * Provides factories for count matchers, time windows, value matchers, pattern builders, and stubbing.
 */
public final class FixMock {
	private FixMock() {}

	// Count matchers
	public static CountMatchingStrategy exactly(int n) {
		return CountMatchingStrategy.exactly(n);
	}

	public static CountMatchingStrategy atLeast(int n) {
		return CountMatchingStrategy.atLeast(n);
	}

	public static CountMatchingStrategy atMost(int n) {
		return CountMatchingStrategy.atMost(n);
	}

	public static CountMatchingStrategy moreThanOrExactly(int n) {
		return CountMatchingStrategy.moreThanOrExactly(n);
	}

	// Time window
	public static TimeWindow within(Duration duration) {
		return TimeWindow.within(duration);
	}

	// Value matchers
	public static ValueMatcher equalTo(String value) {
		return new EqualToMatcher(value);
	}

	public static ValueMatcher matching(String regex) {
		return new RegexMatcher(regex);
	}

	public static ValueMatcher containing(String substring) {
		return new ContainsMatcher(substring);
	}

	public static ValueMatcher in(String... values) {
		return new InMatcher(values);
	}

	public static ValueMatcher absent() {
		return new AbsentMatcher();
	}

	public static ValueMatcher present() {
		return new PresentMatcher();
	}

	public static ValueMatcher anyOf(ValueMatcher... matchers) {
		return new AnyOfMatcher(matchers);
	}

	public static ValueMatcher allOf(ValueMatcher... matchers) {
		return new AllOfMatcher(matchers);
	}

	public static ValueMatcher not(ValueMatcher matcher) {
		return new NotMatcher(matcher);
	}

	// Numeric / range matchers (type-agnostic; parse numbers when possible)
	public static ValueMatcher greaterThan(long value) { return new GreaterThanMatcher(Long.toString(value)); }
	public static ValueMatcher greaterThan(double value) { return new GreaterThanMatcher(Double.toString(value)); }
	public static ValueMatcher greaterThan(String value) { return new GreaterThanMatcher(value); }

	public static ValueMatcher lessThan(long value) { return new LessThanMatcher(Long.toString(value)); }
	public static ValueMatcher lessThan(double value) { return new LessThanMatcher(Double.toString(value)); }
	public static ValueMatcher lessThan(String value) { return new LessThanMatcher(value); }

	public static ValueMatcher between(long minInclusive, long maxInclusive) {
		return new BetweenMatcher(Long.toString(minInclusive), Long.toString(maxInclusive));
	}
	public static ValueMatcher between(double minInclusive, double maxInclusive) {
		return new BetweenMatcher(Double.toString(minInclusive), Double.toString(maxInclusive));
	}
	public static ValueMatcher between(String minInclusive, String maxInclusive) {
		return new BetweenMatcher(minInclusive, maxInclusive);
	}

	// Convenience FIX field matchers (work across FIX versions)
	public static ValueMatcher sideIs(char sideValue) {
		return equalTo(Character.toString(sideValue));
	}
	public static ValueMatcher sideIs(Side side) {
		return sideIs(side.getValue());
	}

	public static ValueMatcher ordTypeIs(char ordTypeValue) {
		return equalTo(Character.toString(ordTypeValue));
	}
	public static ValueMatcher ordTypeIs(OrdType ordType) {
		return ordTypeIs(ordType.getValue());
	}

	public static ValueMatcher execTypeIs(char execTypeValue) {
		return equalTo(Character.toString(execTypeValue));
	}
	public static ValueMatcher execTypeIs(ExecType execType) {
		return execTypeIs(execType.getValue());
	}

	// Pattern builders for verification
	public static FixMessagePatternBuilder appMessageRequestedFor() {
		return FixMessagePatternBuilder.app();
	}

	public static FixMessagePatternBuilder adminMessageRequestedFor() {
		return FixMessagePatternBuilder.admin();
	}

	public static FixMessagePatternBuilder messageType(String msgType) {
		return FixMessagePatternBuilder.any().messageType(msgType);
	}

	public static FixMessagePatternBuilder newOrderSingle() {
		return appMessageRequestedFor().newOrderSingle();
	}

	public static FixMessagePatternBuilder executionReport() {
		return appMessageRequestedFor().executionReport();
	}

	public static FixMessagePatternBuilder orderCancelRequest() {
		return appMessageRequestedFor().orderCancelRequest();
	}

	public static FixMessagePatternBuilder orderCancelReplaceRequest() {
		return appMessageRequestedFor().orderCancelReplaceRequest();
	}

	public static FixMessagePatternBuilder orderCancelReject() {
		return appMessageRequestedFor().orderCancelReject();
	}

	public static FixMessagePatternBuilder marketDataRequest() {
		return appMessageRequestedFor().marketDataRequest();
	}

	public static FixMessagePatternBuilder marketDataSnapshotFullRefresh() {
		return appMessageRequestedFor().marketDataSnapshotFullRefresh();
	}

	public static FixMessagePatternBuilder marketDataIncrementalRefresh() {
		return appMessageRequestedFor().marketDataIncrementalRefresh();
	}

	public static FixMessagePatternBuilder securityListRequest() {
		return appMessageRequestedFor().securityListRequest();
	}

	public static FixMessagePatternBuilder securityList() {
		return appMessageRequestedFor().securityList();
	}

	public static FixMessagePatternBuilder tradeCaptureReport() {
		return appMessageRequestedFor().tradeCaptureReport();
	}

	public static FixMessagePatternBuilder tradeCaptureReportAck() {
		return appMessageRequestedFor().tradeCaptureReportAck();
	}

	public static FixMessagePatternBuilder logonRequested() {
		return adminMessageRequestedFor().logon();
	}

	public static FixMessagePatternBuilder logoutRequested() {
		return adminMessageRequestedFor().logout();
	}

	public static FixMessagePatternBuilder heartbeatRequested() {
		return adminMessageRequestedFor().heartbeat();
	}

	public static FixMessagePatternBuilder testRequestRequested() {
		return adminMessageRequestedFor().testRequest();
	}

	public static FixMessagePatternBuilder resendRequestRequested() {
		return adminMessageRequestedFor().resendRequest();
	}

	public static FixMessagePatternBuilder sequenceResetRequested() {
		return adminMessageRequestedFor().sequenceReset();
	}

	public static FixMessagePatternBuilder sessionId(String sessionId) {
		return FixMessagePatternBuilder.any().sessionId(sessionId);
	}

	// Group selectors
	public static GroupElementSelector anyElement() {
		return GroupElementSelector.anyElement();
	}

	public static GroupElementSelector elementAt(int index) {
		return GroupElementSelector.elementAt(index);
	}

	// Stubbing DSL

	/**
	 * Creates a stub mapping builder for the given request pattern.
	 * Prefer using {@link #when(FixMessagePattern)} for more readable code.
	 *
	 * @param requestPattern the pattern to match incoming messages
	 * @return a builder to configure the stub response
	 * @see #when(FixMessagePattern)
	 */
	public static StubMappingBuilder stubFor(FixMessagePattern requestPattern) {
		return new StubMappingBuilder(requestPattern);
	}

	/**
	 * Creates a stub mapping builder for the given request pattern.
	 * This is an alias for {@link #stubFor(FixMessagePattern)} that reads more naturally:
	 * <pre>
	 * server.stubFor(when(newOrderSingle()).willReturn(respondWith(er)));
	 * </pre>
	 *
	 * @param requestPattern the pattern to match incoming messages
	 * @return a builder to configure the stub response
	 */
	public static StubMappingBuilder when(FixMessagePattern requestPattern) {
		return new StubMappingBuilder(requestPattern);
	}

	/**
	 * Creates a static response definition wrapping the given message.
	 *
	 * @param response the FIX message to send as response
	 * @return a response definition that sends the provided message
	 */
	public static ResponseDefinition respondWith(Message response) {
		return new StaticResponseDefinition(response);
	}

	// ============================================================================================
	// Admin message convenience factories
	//
	// NOTE: These methods currently return FIX 4.4 admin messages. For other FIX versions,
	// use respondWith() with the appropriate message type from the QuickFIX/J library:
	//   - FIX 4.2: quickfix.fix42.Logon, quickfix.fix42.Logout, etc.
	//   - FIX 5.0: quickfix.fix50.Logon, quickfix.fix50.Logout, etc.
	// ============================================================================================

	/**
	 * Creates a Logon response (FIX 4.4).
	 * For other FIX versions, use {@code respondWith(new quickfix.fixXX.Logon())}.
	 */
	public static ResponseDefinition logonResponse() {
		return respondWith(new quickfix.fix44.Logon());
	}

	/**
	 * Creates a Logout response (FIX 4.4).
	 * For other FIX versions, use {@code respondWith(new quickfix.fixXX.Logout())}.
	 */
	public static ResponseDefinition logoutResponse() {
		return respondWith(new quickfix.fix44.Logout());
	}

	/**
	 * Creates a Heartbeat response (FIX 4.4).
	 * For other FIX versions, use {@code respondWith(new quickfix.fixXX.Heartbeat())}.
	 */
	public static ResponseDefinition heartbeat() {
		return respondWith(new quickfix.fix44.Heartbeat());
	}

	/**
	 * Creates a Heartbeat response with TestReqID (FIX 4.4).
	 * For other FIX versions, use {@code respondWith()} with the appropriate Heartbeat type.
	 */
	public static ResponseDefinition testRequestResponse(String testReqID) {
		quickfix.fix44.Heartbeat heartbeat = new quickfix.fix44.Heartbeat();
		heartbeat.set(new quickfix.field.TestReqID(testReqID));
		return respondWith(heartbeat);
	}

	/**
	 * Creates a TestRequest message (FIX 4.4).
	 * For other FIX versions, use {@code respondWith()} with the appropriate TestRequest type.
	 */
	public static ResponseDefinition testRequest(String testReqID) {
		quickfix.fix44.TestRequest tr = new quickfix.fix44.TestRequest();
		tr.set(new quickfix.field.TestReqID(testReqID));
		return respondWith(tr);
	}

	/**
	 * Creates a SequenceReset message (FIX 4.4).
	 * For other FIX versions, use {@code respondWith()} with the appropriate SequenceReset type.
	 */
	public static ResponseDefinition sequenceReset(int newSeqNo, boolean gapFill) {
		quickfix.fix44.SequenceReset reset = new quickfix.fix44.SequenceReset();
		reset.set(new quickfix.field.NewSeqNo(newSeqNo));
		reset.set(new quickfix.field.GapFillFlag(gapFill));
		return respondWith(reset);
	}

	public static ResponseDefinition resendRequest(int beginSeqNo, int endSeqNo) {
		quickfix.fix44.ResendRequest req = new quickfix.fix44.ResendRequest();
		req.set(new quickfix.field.BeginSeqNo(beginSeqNo));
		req.set(new quickfix.field.EndSeqNo(endSeqNo));
		return respondWith(req);
	}

	/** Convenience: static response with delay. */
	public static ResponseDefinition respondWith(Message response, Duration delay) {
		long ms = delay == null ? 0 : delay.toMillis();
		return new StaticResponseDefinition(response, ms, false);
	}

	/**
	 * Start a templated response builder that will create a fresh response per request and optionally
	 * copy tag values from the request into the response.
	 */
	public static ResponseTemplateBuilder respondWithTemplate(Supplier<Message> responseSupplier) {
		return new ResponseTemplateBuilder(responseSupplier);
	}

	/** Build a simple response sequence: each match returns the next response in order (last repeats). */
	public static ResponseDefinition sequence(ResponseDefinition... responses) {
		return new SequenceResponseDefinition(java.util.Arrays.asList(responses));
	}

	/** Convenience to mark a response as disconnecting after sending. */
	public static ResponseDefinition disconnectAfter(ResponseDefinition response) {
		if (response instanceof ResponseTemplateBuilder rtb) {
			return rtb.disconnect();
		}
		// Wrap static response into a StaticResponseDefinition with disconnect flag
		if (response instanceof StaticResponseDefinition srd) {
			return new StaticResponseDefinition(srd.generateResponse(null), srd.getDelayMillis(), true);
		}
		// Fallback: build a templated wrapper that forwards generateResponse and disconnects
		return new ResponseDefinition() {
			@Override
			public Message generateResponse(Message request) throws Exception { return response.generateResponse(request); }
			@Override
			public long getDelayMillis() { return response.getDelayMillis(); }
			@Override
			public boolean shouldDisconnect() { return true; }
			@Override
			public String describe() { return response.describe() + "+disconnect"; }
		};
	}

	/**
	 * Fluent builder for creating StubMapping instances.
	 * Usage: stubFor(pattern).willReturn(response)
	 */
	public static final class StubMappingBuilder {
		private final FixMessagePattern requestPattern;
		private int priority = 5;
		private String scenarioName;
		private String requiredScenarioState;
		private String newScenarioState;

		private StubMappingBuilder(FixMessagePattern requestPattern) {
			this.requestPattern = requestPattern;
		}

		public StubMappingBuilder atPriority(int priority) {
			this.priority = priority;
			return this;
		}

		public StubMappingBuilder inScenario(String scenarioName) {
			this.scenarioName = scenarioName;
			return this;
		}

		public StubMappingBuilder whenScenarioStateIs(String state) {
			this.requiredScenarioState = state;
			return this;
		}

		public StubMappingBuilder willSetStateTo(String state) {
			this.newScenarioState = state;
			return this;
		}

		public StubMapping willReturn(ResponseDefinition response) {
			return new StubMapping(requestPattern, response, priority, scenarioName, requiredScenarioState, newScenarioState);
		}

		public StubMapping willReturn(Message response) {
			return willReturn(new StaticResponseDefinition(response));
		}
	}

	/**
	 * Builder implementing a templated ResponseDefinition with optional tag copying, delay, and disconnect.
	 */
	public static final class ResponseTemplateBuilder implements ResponseDefinition {
		private final Supplier<Message> supplier;
		private int[] tagsToCopy = new int[0];
		private int[] groupTagsToCopy = new int[0];
		private long delayMillis = 0;
		private boolean disconnect = false;

		private ResponseTemplateBuilder(Supplier<Message> supplier) {
			this.supplier = supplier;
		}

		public ResponseTemplateBuilder copyFromRequestTags(int... tags) {
			this.tagsToCopy = tags == null ? new int[0] : Arrays.copyOf(tags, tags.length);
			return this;
		}

		/**
		 * Copies repeating groups from the request message to the response.
		 *
		 * @param groupTags the group counter tags to copy (e.g., 453 for NoPartyIDs)
		 * @return this builder
		 */
		public ResponseTemplateBuilder copyGroupsFromRequest(int... groupTags) {
			this.groupTagsToCopy = groupTags == null ? new int[0] : Arrays.copyOf(groupTags, groupTags.length);
			return this;
		}

		public ResponseTemplateBuilder delay(Duration duration) {
			this.delayMillis = duration == null ? 0 : duration.toMillis();
			return this;
		}

		public ResponseTemplateBuilder disconnect() {
			this.disconnect = true;
			return this;
		}

		@Override
		public Message generateResponse(Message request) throws Exception {
			Message response = supplier.get();
			if (response == null) return null;
			// Copy requested tags (header/body) from request to response
			if (tagsToCopy != null) {
				for (int tag : tagsToCopy) {
					try {
						if (request.isSetField(tag)) {
							String value = request.getString(tag);
							response.setString(tag, value);
						} else if (request.getHeader().isSetField(tag)) {
							String value = request.getHeader().getString(tag);
							response.getHeader().setString(tag, value);
						} else if (request.getTrailer().isSetField(tag)) {
							String value = request.getTrailer().getString(tag);
							response.getTrailer().setString(tag, value);
						}
					} catch (Exception ignore) {
						// Best-effort copy; ignore if tag not present or unsupported
					}
				}
			}
			// Copy repeating groups from request to response
			for (int groupTag : groupTagsToCopy) {
				try {
					java.util.List<quickfix.Group> groups = request.getGroups(groupTag);
					if (groups != null) {
						for (quickfix.Group g : groups) {
							response.addGroup(g);
						}
					}
				} catch (Exception ignore) {
					// Best-effort copy; ignore if group not present
				}
			}
			return response;
		}

		@Override
		public long getDelayMillis() {
			return delayMillis;
		}

		@Override
		public boolean shouldDisconnect() {
			return disconnect;
		}

		@Override
		public String describe() {
			return "TemplatedResponse[copyTags=" + Arrays.toString(tagsToCopy) + ", copyGroups=" + Arrays.toString(groupTagsToCopy) + ", delay=" + delayMillis + "ms, disconnect=" + disconnect + "]";
		}
	}
}
