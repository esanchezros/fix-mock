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

import io.allune.fixmock.matching.FixMessagePatternBuilder;
import io.allune.fixmock.stubbing.StubMapping;
import io.allune.fixmock.verify.Direction;
import io.allune.fixmock.verify.ValueMatcher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import quickfix.Message;
import quickfix.field.ExecType;
import quickfix.field.MsgType;
import quickfix.field.OrdType;
import quickfix.field.Side;

import static org.junit.jupiter.api.Assertions.*;

class FixMockConvenienceMatchersTest {

	@Nested
	class FieldConvenienceMatchers {

		@Test
		void shouldMatchSideByChar() {
			ValueMatcher matcher = FixMock.sideIs('1');

			assertTrue(matcher.matches("1"));
			assertFalse(matcher.matches("2"));
		}

		@Test
		void shouldMatchSideBySideEnum() {
			ValueMatcher matcher = FixMock.sideIs(new Side(Side.BUY));

			assertTrue(matcher.matches("1"));
			assertFalse(matcher.matches("2"));
		}

		@Test
		void shouldMatchOrdTypeByChar() {
			ValueMatcher matcher = FixMock.ordTypeIs('2');

			assertTrue(matcher.matches("2"));
			assertFalse(matcher.matches("1"));
		}

		@Test
		void shouldMatchOrdTypeByOrdTypeEnum() {
			ValueMatcher matcher = FixMock.ordTypeIs(new OrdType(OrdType.LIMIT));

			assertTrue(matcher.matches("2"));
			assertFalse(matcher.matches("1"));
		}

		@Test
		void shouldMatchExecTypeByChar() {
			ValueMatcher matcher = FixMock.execTypeIs('0');

			assertTrue(matcher.matches("0"));
			assertFalse(matcher.matches("1"));
		}

		@Test
		void shouldMatchExecTypeByExecTypeEnum() {
			ValueMatcher matcher = FixMock.execTypeIs(new ExecType(ExecType.NEW));

			assertTrue(matcher.matches("0"));
			assertFalse(matcher.matches("F"));
		}
	}

	@Nested
	class PatternBuilderFactories {

		@Test
		void shouldCreateAppDirectionBuilder() {
			FixMessagePatternBuilder builder = FixMock.appMessageRequestedFor();

			assertEquals(Direction.APP, builder.getDirection());
		}

		@Test
		void shouldCreateAdminDirectionBuilder() {
			FixMessagePatternBuilder builder = FixMock.adminMessageRequestedFor();

			assertEquals(Direction.ADMIN, builder.getDirection());
		}

		@Test
		void shouldCreateLogonRequestedPattern() {
			FixMessagePatternBuilder builder = FixMock.logonRequested();

			assertEquals(Direction.ADMIN, builder.getDirection());
			assertEquals(MsgType.LOGON, builder.getMsgType());
		}

		@Test
		void shouldCreateLogoutRequestedPattern() {
			FixMessagePatternBuilder builder = FixMock.logoutRequested();

			assertEquals(Direction.ADMIN, builder.getDirection());
			assertEquals(MsgType.LOGOUT, builder.getMsgType());
		}

		@Test
		void shouldCreateHeartbeatRequestedPattern() {
			FixMessagePatternBuilder builder = FixMock.heartbeatRequested();

			assertEquals(Direction.ADMIN, builder.getDirection());
			assertEquals(MsgType.HEARTBEAT, builder.getMsgType());
		}

		@Test
		void shouldCreateTestRequestRequestedPattern() {
			FixMessagePatternBuilder builder = FixMock.testRequestRequested();

			assertEquals(Direction.ADMIN, builder.getDirection());
			assertEquals(MsgType.TEST_REQUEST, builder.getMsgType());
		}

		@Test
		void shouldCreateResendRequestRequestedPattern() {
			FixMessagePatternBuilder builder = FixMock.resendRequestRequested();

			assertEquals(Direction.ADMIN, builder.getDirection());
			assertEquals(MsgType.RESEND_REQUEST, builder.getMsgType());
		}

		@Test
		void shouldCreateSequenceResetRequestedPattern() {
			FixMessagePatternBuilder builder = FixMock.sequenceResetRequested();

			assertEquals(Direction.ADMIN, builder.getDirection());
			assertEquals(MsgType.SEQUENCE_RESET, builder.getMsgType());
		}

		@Test
		void shouldCreatePatternWithSessionId() {
			FixMessagePatternBuilder builder = FixMock.sessionId("FIX.4.4:CLIENT->BROKER");

			assertEquals("FIX.4.4:CLIENT->BROKER", builder.getSessionId());
		}

		@Test
		void shouldCreateOrderCancelRequestPattern() {
			FixMessagePatternBuilder builder = FixMock.orderCancelRequest();

			assertEquals(MsgType.ORDER_CANCEL_REQUEST, builder.getMsgType());
		}

		@Test
		void shouldCreateOrderCancelReplaceRequestPattern() {
			FixMessagePatternBuilder builder = FixMock.orderCancelReplaceRequest();

			assertEquals(MsgType.ORDER_CANCEL_REPLACE_REQUEST, builder.getMsgType());
		}

		@Test
		void shouldCreateOrderCancelRejectPattern() {
			FixMessagePatternBuilder builder = FixMock.orderCancelReject();

			assertEquals(MsgType.ORDER_CANCEL_REJECT, builder.getMsgType());
		}

		@Test
		void shouldCreateMarketDataRequestPattern() {
			FixMessagePatternBuilder builder = FixMock.marketDataRequest();

			assertEquals(MsgType.MARKET_DATA_REQUEST, builder.getMsgType());
		}

		@Test
		void shouldCreateMarketDataSnapshotFullRefreshPattern() {
			FixMessagePatternBuilder builder = FixMock.marketDataSnapshotFullRefresh();

			assertEquals(MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH, builder.getMsgType());
		}

		@Test
		void shouldCreateMarketDataIncrementalRefreshPattern() {
			FixMessagePatternBuilder builder = FixMock.marketDataIncrementalRefresh();

			assertEquals(MsgType.MARKET_DATA_INCREMENTAL_REFRESH, builder.getMsgType());
		}

		@Test
		void shouldCreateSecurityListRequestPattern() {
			FixMessagePatternBuilder builder = FixMock.securityListRequest();

			assertEquals(MsgType.SECURITY_LIST_REQUEST, builder.getMsgType());
		}

		@Test
		void shouldCreateSecurityListPattern() {
			FixMessagePatternBuilder builder = FixMock.securityList();

			assertEquals(MsgType.SECURITY_LIST, builder.getMsgType());
		}
	}

	@Nested
	class GroupSelectors {

		@Test
		void shouldCreateAnyElementSelector() {
			FixMessagePatternBuilder.GroupElementSelector selector = FixMock.anyElement();

			assertNotNull(selector);
			assertTrue(selector.isAnyMode());
			assertFalse(selector.isIndexMode());
		}

		@Test
		void shouldCreateElementAtSelector() {
			FixMessagePatternBuilder.GroupElementSelector selector = FixMock.elementAt(0);

			assertNotNull(selector);
			assertTrue(selector.isIndexMode());
			assertFalse(selector.isAnyMode());
			assertEquals(0, selector.getIndex());
		}

		@Test
		void shouldCreateElementAtSelectorWithHigherIndex() {
			FixMessagePatternBuilder.GroupElementSelector selector = FixMock.elementAt(5);

			assertEquals(5, selector.getIndex());
		}
	}

	@Nested
	class StubMappingBuilderTests {

		@Test
		void shouldSetPriority() {
			StubMapping mapping = FixMock.stubFor(FixMock.newOrderSingle())
					.atPriority(1)
					.willReturn(FixMock.respondWith(new quickfix.fix44.ExecutionReport()));

			assertEquals(1, mapping.getPriority());
		}

		@Test
		void shouldDefaultPriorityTo5() {
			StubMapping mapping = FixMock.stubFor(FixMock.newOrderSingle())
					.willReturn(FixMock.respondWith(new quickfix.fix44.ExecutionReport()));

			assertEquals(5, mapping.getPriority());
		}

		@Test
		void shouldSetScenarioName() {
			StubMapping mapping = FixMock.stubFor(FixMock.newOrderSingle())
					.inScenario("order-flow")
					.willReturn(FixMock.respondWith(new quickfix.fix44.ExecutionReport()));

			assertEquals("order-flow", mapping.getScenarioName());
			assertTrue(mapping.isInScenario());
		}

		@Test
		void shouldSetRequiredScenarioState() {
			StubMapping mapping = FixMock.stubFor(FixMock.newOrderSingle())
					.inScenario("order-flow")
					.whenScenarioStateIs("PENDING")
					.willReturn(FixMock.respondWith(new quickfix.fix44.ExecutionReport()));

			assertEquals("PENDING", mapping.getRequiredScenarioState());
		}

		@Test
		void shouldSetNewScenarioState() {
			StubMapping mapping = FixMock.stubFor(FixMock.newOrderSingle())
					.inScenario("order-flow")
					.willSetStateTo("FILLED")
					.willReturn(FixMock.respondWith(new quickfix.fix44.ExecutionReport()));

			assertEquals("FILLED", mapping.getNewScenarioState());
		}

		@Test
		void shouldAcceptMessageDirectlyInWillReturn() {
			Message response = new quickfix.fix44.ExecutionReport();
			StubMapping mapping = FixMock.stubFor(FixMock.newOrderSingle())
					.willReturn(response);

			assertNotNull(mapping.getResponse());
		}

		@Test
		void shouldNotBeInScenarioByDefault() {
			StubMapping mapping = FixMock.stubFor(FixMock.newOrderSingle())
					.willReturn(FixMock.respondWith(new quickfix.fix44.ExecutionReport()));

			assertFalse(mapping.isInScenario());
			assertNull(mapping.getScenarioName());
			assertNull(mapping.getRequiredScenarioState());
			assertNull(mapping.getNewScenarioState());
		}

		@Test
		void shouldChainAllScenarioMethods() {
			StubMapping mapping = FixMock.when(FixMock.newOrderSingle())
					.atPriority(2)
					.inScenario("trade")
					.whenScenarioStateIs("NEW")
					.willSetStateTo("ACKED")
					.willReturn(FixMock.respondWith(new quickfix.fix44.ExecutionReport()));

			assertEquals(2, mapping.getPriority());
			assertEquals("trade", mapping.getScenarioName());
			assertEquals("NEW", mapping.getRequiredScenarioState());
			assertEquals("ACKED", mapping.getNewScenarioState());
			assertTrue(mapping.isInScenario());
		}
	}

	@Nested
	class MoreThanOrExactlyMatcher {

		@Test
		void shouldMatchMoreThanOrExactly() {
			var strategy = FixMock.moreThanOrExactly(3);

			assertTrue(strategy.matches(3));
			assertTrue(strategy.matches(4));
			assertFalse(strategy.matches(2));
		}
	}
}
