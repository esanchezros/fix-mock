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
package io.allune.fixmock.stubbing;

import org.junit.jupiter.api.Test;
import quickfix.Message;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SequenceResponseDefinitionTest {

	@Test
	void shouldReturnFirstResponseOnFirstCall() throws Exception {
		Message first = new Message();
		first.getHeader().setString(35, "8");
		Message second = new Message();
		second.getHeader().setString(35, "9");

		SequenceResponseDefinition sequence = new SequenceResponseDefinition(List.of(
				new StaticResponseDefinition(first),
				new StaticResponseDefinition(second)
		));

		Message request = new Message();
		assertSame(first, sequence.generateResponse(request));
	}

	@Test
	void shouldReturnSecondResponseOnSecondCall() throws Exception {
		Message first = new Message();
		first.getHeader().setString(35, "8");
		Message second = new Message();
		second.getHeader().setString(35, "9");

		SequenceResponseDefinition sequence = new SequenceResponseDefinition(List.of(
				new StaticResponseDefinition(first),
				new StaticResponseDefinition(second)
		));

		Message request = new Message();
		sequence.generateResponse(request); // first call
		assertSame(second, sequence.generateResponse(request));
	}

	@Test
	void shouldRepeatLastResponseAfterExhaustion() throws Exception {
		Message first = new Message();
		first.getHeader().setString(35, "8");
		Message second = new Message();
		second.getHeader().setString(35, "9");

		SequenceResponseDefinition sequence = new SequenceResponseDefinition(List.of(
				new StaticResponseDefinition(first),
				new StaticResponseDefinition(second)
		));

		Message request = new Message();
		sequence.generateResponse(request); // first
		sequence.generateResponse(request); // second
		assertSame(second, sequence.generateResponse(request)); // still second
		assertSame(second, sequence.generateResponse(request)); // still second
	}

	@Test
	void shouldThrowIllegalArgumentExceptionForEmptyList() {
		assertThrows(IllegalArgumentException.class,
				() -> new SequenceResponseDefinition(Collections.emptyList()));
	}

	@Test
	void describeShouldReturnCorrectFormat() {
		Message msg = new Message();
		SequenceResponseDefinition sequence = new SequenceResponseDefinition(List.of(
				new StaticResponseDefinition(msg),
				new StaticResponseDefinition(msg),
				new StaticResponseDefinition(msg)
		));

		assertEquals("SequenceResponse[steps=3]", sequence.describe());
	}
}
