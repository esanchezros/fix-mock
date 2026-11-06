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

import io.allune.fixmock.matching.AbsentMatcher;
import io.allune.fixmock.matching.AllOfMatcher;
import io.allune.fixmock.matching.AnyOfMatcher;
import io.allune.fixmock.matching.BetweenMatcher;
import io.allune.fixmock.matching.ContainsMatcher;
import io.allune.fixmock.matching.EqualToMatcher;
import io.allune.fixmock.matching.FixMessagePatternBuilder;
import io.allune.fixmock.matching.GreaterThanMatcher;
import io.allune.fixmock.matching.InMatcher;
import io.allune.fixmock.matching.LessThanMatcher;
import io.allune.fixmock.matching.NotMatcher;
import io.allune.fixmock.matching.PresentMatcher;
import io.allune.fixmock.matching.RegexMatcher;
import io.allune.fixmock.stubbing.ResponseDefinition;
import io.allune.fixmock.stubbing.SequenceResponseDefinition;
import io.allune.fixmock.stubbing.StaticResponseDefinition;
import io.allune.fixmock.stubbing.StubMapping;
import io.allune.fixmock.verify.CountMatchingStrategy;
import io.allune.fixmock.verify.FixMessagePattern;
import io.allune.fixmock.verify.ValueMatcher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import quickfix.Message;
import quickfix.field.MsgType;

import static org.junit.jupiter.api.Assertions.*;

class FixMockTest {

	// ========================================================================================
	// Value matchers
	// ========================================================================================

	@Nested
	class ValueMatchers {

		@Test
		void shouldReturnEqualToMatcherThatMatches() {
			ValueMatcher matcher = FixMock.equalTo("AAPL");
			assertInstanceOf(EqualToMatcher.class, matcher);
			assertTrue(matcher.matches("AAPL"));
			assertFalse(matcher.matches("GOOG"));
		}

		@Test
		void shouldReturnRegexMatcherThatMatches() {
			ValueMatcher matcher = FixMock.matching("ORD-\\d+");
			assertInstanceOf(RegexMatcher.class, matcher);
			assertTrue(matcher.matches("ORD-123"));
			assertFalse(matcher.matches("TRADE-123"));
		}

		@Test
		void shouldReturnContainsMatcherThatMatches() {
			ValueMatcher matcher = FixMock.containing("USD");
			assertInstanceOf(ContainsMatcher.class, matcher);
			assertTrue(matcher.matches("EUR/USD"));
			assertFalse(matcher.matches("EUR/GBP"));
		}

		@Test
		void shouldReturnInMatcherThatMatches() {
			ValueMatcher matcher = FixMock.in("1", "2", "3");
			assertInstanceOf(InMatcher.class, matcher);
			assertTrue(matcher.matches("1"));
			assertTrue(matcher.matches("2"));
			assertFalse(matcher.matches("4"));
		}

		@Test
		void shouldReturnAbsentMatcherThatMatchesNull() {
			ValueMatcher matcher = FixMock.absent();
			assertInstanceOf(AbsentMatcher.class, matcher);
			assertTrue(matcher.matches(null));
			assertFalse(matcher.matches("something"));
		}

		@Test
		void shouldReturnPresentMatcherThatMatchesNonNull() {
			ValueMatcher matcher = FixMock.present();
			assertInstanceOf(PresentMatcher.class, matcher);
			assertTrue(matcher.matches("something"));
			assertFalse(matcher.matches(null));
		}

		@Test
		void shouldNegateMatcherWithNot() {
			ValueMatcher matcher = FixMock.not(FixMock.equalTo("AAPL"));
			assertInstanceOf(NotMatcher.class, matcher);
			assertTrue(matcher.matches("GOOG"));
			assertFalse(matcher.matches("AAPL"));
		}

		@Test
		void shouldMatchWhenAnyOfChildrenMatches() {
			ValueMatcher matcher = FixMock.anyOf(FixMock.equalTo("A"), FixMock.equalTo("B"));
			assertInstanceOf(AnyOfMatcher.class, matcher);
			assertTrue(matcher.matches("A"));
			assertTrue(matcher.matches("B"));
			assertFalse(matcher.matches("C"));
		}

		@Test
		void shouldMatchWhenAllOfChildrenMatch() {
			ValueMatcher matcher = FixMock.allOf(FixMock.containing("USD"), FixMock.containing("EUR"));
			assertInstanceOf(AllOfMatcher.class, matcher);
			assertTrue(matcher.matches("EUR/USD"));
			assertFalse(matcher.matches("EUR/GBP"));
		}
	}

	// ========================================================================================
	// Numeric / range matchers
	// ========================================================================================

	@Nested
	class NumericMatchers {

		@Test
		void shouldMatchGreaterThanLong() {
			ValueMatcher matcher = FixMock.greaterThan(100L);
			assertInstanceOf(GreaterThanMatcher.class, matcher);
			assertTrue(matcher.matches("101"));
			assertFalse(matcher.matches("100"));
			assertFalse(matcher.matches("99"));
		}

		@Test
		void shouldMatchGreaterThanDouble() {
			ValueMatcher matcher = FixMock.greaterThan(1.5);
			assertInstanceOf(GreaterThanMatcher.class, matcher);
			assertTrue(matcher.matches("1.6"));
			assertFalse(matcher.matches("1.5"));
			assertFalse(matcher.matches("1.4"));
		}

		@Test
		void shouldMatchGreaterThanString() {
			ValueMatcher matcher = FixMock.greaterThan("b");
			assertInstanceOf(GreaterThanMatcher.class, matcher);
			assertTrue(matcher.matches("c"));
			assertFalse(matcher.matches("b"));
			assertFalse(matcher.matches("a"));
		}

		@Test
		void shouldMatchLessThanLong() {
			ValueMatcher matcher = FixMock.lessThan(100L);
			assertInstanceOf(LessThanMatcher.class, matcher);
			assertTrue(matcher.matches("99"));
			assertFalse(matcher.matches("100"));
			assertFalse(matcher.matches("101"));
		}

		@Test
		void shouldMatchLessThanDouble() {
			ValueMatcher matcher = FixMock.lessThan(1.5);
			assertInstanceOf(LessThanMatcher.class, matcher);
			assertTrue(matcher.matches("1.4"));
			assertFalse(matcher.matches("1.5"));
			assertFalse(matcher.matches("1.6"));
		}

		@Test
		void shouldMatchLessThanString() {
			ValueMatcher matcher = FixMock.lessThan("b");
			assertInstanceOf(LessThanMatcher.class, matcher);
			assertTrue(matcher.matches("a"));
			assertFalse(matcher.matches("b"));
			assertFalse(matcher.matches("c"));
		}

		@Test
		void shouldMatchBetweenLongInclusive() {
			ValueMatcher matcher = FixMock.between(10L, 20L);
			assertInstanceOf(BetweenMatcher.class, matcher);
			assertTrue(matcher.matches("10"));
			assertTrue(matcher.matches("15"));
			assertTrue(matcher.matches("20"));
			assertFalse(matcher.matches("9"));
			assertFalse(matcher.matches("21"));
		}

		@Test
		void shouldMatchBetweenDoubleInclusive() {
			ValueMatcher matcher = FixMock.between(1.0, 2.0);
			assertInstanceOf(BetweenMatcher.class, matcher);
			assertTrue(matcher.matches("1.0"));
			assertTrue(matcher.matches("1.5"));
			assertTrue(matcher.matches("2.0"));
			assertFalse(matcher.matches("0.9"));
			assertFalse(matcher.matches("2.1"));
		}

		@Test
		void shouldMatchBetweenStringInclusive() {
			ValueMatcher matcher = FixMock.between("aa", "az");
			assertInstanceOf(BetweenMatcher.class, matcher);
			assertTrue(matcher.matches("aa"));
			assertTrue(matcher.matches("am"));
			assertTrue(matcher.matches("az"));
			assertFalse(matcher.matches("a"));
			assertFalse(matcher.matches("b"));
		}
	}

	// ========================================================================================
	// Count matchers
	// ========================================================================================

	@Nested
	class CountMatchers {

		@Test
		void shouldMatchExactlyWhenCountEquals() {
			CountMatchingStrategy strategy = FixMock.exactly(1);
			assertTrue(strategy.matches(1));
			assertFalse(strategy.matches(0));
			assertFalse(strategy.matches(2));
		}

		@Test
		void shouldMatchAtLeastWhenCountIsGreaterOrEqual() {
			CountMatchingStrategy strategy = FixMock.atLeast(2);
			assertTrue(strategy.matches(2));
			assertTrue(strategy.matches(3));
			assertFalse(strategy.matches(1));
		}

		@Test
		void shouldMatchAtMostWhenCountIsLessOrEqual() {
			CountMatchingStrategy strategy = FixMock.atMost(3);
			assertTrue(strategy.matches(3));
			assertTrue(strategy.matches(2));
			assertFalse(strategy.matches(4));
		}
	}

	// ========================================================================================
	// Pattern builders
	// ========================================================================================

	@Nested
	class PatternBuilders {

		@Test
		void shouldCreateNewOrderSinglePatternWithMsgTypeD() {
			FixMessagePatternBuilder builder = FixMock.newOrderSingle();
			assertEquals("D", builder.getMsgType());
		}

		@Test
		void shouldCreateExecutionReportPatternWithMsgType8() {
			FixMessagePatternBuilder builder = FixMock.executionReport();
			assertEquals(MsgType.EXECUTION_REPORT, builder.getMsgType());
		}

		@Test
		void shouldCreateTradeCaptureReportPatternWithMsgTypeAE() {
			FixMessagePatternBuilder builder = FixMock.tradeCaptureReport();
			assertEquals(MsgType.TRADE_CAPTURE_REPORT, builder.getMsgType());
		}

		@Test
		void shouldCreateTradeCaptureReportAckPatternWithMsgTypeAR() {
			FixMessagePatternBuilder builder = FixMock.tradeCaptureReportAck();
			assertEquals(MsgType.TRADE_CAPTURE_REPORT_ACK, builder.getMsgType());
		}

		@Test
		void shouldCreatePatternWithArbitraryMsgType() {
			FixMessagePatternBuilder builder = FixMock.messageType("X");
			assertEquals("X", builder.getMsgType());
		}
	}

	// ========================================================================================
	// Stubbing DSL
	// ========================================================================================

	@Nested
	class StubbingDsl {

		@Test
		void shouldReturnStubMappingBuilderFromStubFor() {
			FixMessagePattern pattern = FixMock.newOrderSingle();
			FixMock.StubMappingBuilder builder = FixMock.stubFor(pattern);
			assertNotNull(builder);
		}

		@Test
		void shouldReturnStubMappingBuilderFromWhen() {
			FixMessagePattern pattern = FixMock.newOrderSingle();
			FixMock.StubMappingBuilder builder = FixMock.when(pattern);
			assertNotNull(builder);
		}

		@Test
		void shouldCreateStubMappingViaWillReturn() {
			FixMessagePattern pattern = FixMock.newOrderSingle();
			Message response = new quickfix.fix44.ExecutionReport();
			StubMapping mapping = FixMock.stubFor(pattern).willReturn(FixMock.respondWith(response));
			assertNotNull(mapping);
			assertNotNull(mapping.getRequestPattern());
			assertNotNull(mapping.getResponse());
		}

		@Test
		void shouldReturnStaticResponseDefinitionFromRespondWith() {
			Message msg = new quickfix.fix44.ExecutionReport();
			ResponseDefinition response = FixMock.respondWith(msg);
			assertInstanceOf(StaticResponseDefinition.class, response);
		}

		@Test
		void shouldReturnSequenceResponseDefinitionFromSequence() {
			Message msg1 = new quickfix.fix44.ExecutionReport();
			Message msg2 = new quickfix.fix44.ExecutionReport();
			ResponseDefinition response = FixMock.sequence(
					FixMock.respondWith(msg1),
					FixMock.respondWith(msg2)
			);
			assertInstanceOf(SequenceResponseDefinition.class, response);
		}
	}
}
