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

import static org.junit.jupiter.api.Assertions.*;

class StaticResponseDefinitionTest {

	@Test
	void generateResponseShouldReturnTheFixedMessage() {
		Message response = new Message();
		response.getHeader().setString(35, "8");
		StaticResponseDefinition definition = new StaticResponseDefinition(response);

		Message request = new Message();
		request.getHeader().setString(35, "D");

		assertSame(response, definition.generateResponse(request));
	}

	@Test
	void getDelayMillisShouldReturnConfiguredDelay() {
		Message response = new Message();
		StaticResponseDefinition definition = new StaticResponseDefinition(response, 500, false);
		assertEquals(500, definition.getDelayMillis());
	}

	@Test
	void shouldDisconnectShouldReturnConfiguredValue() {
		Message response = new Message();
		StaticResponseDefinition definition = new StaticResponseDefinition(response, 0, true);
		assertTrue(definition.shouldDisconnect());
	}

	@Test
	void describeShouldIncludeMsgTypeFromHeaderTag35() {
		Message msg = new Message();
		msg.getHeader().setString(35, "D");
		StaticResponseDefinition definition = new StaticResponseDefinition(msg, 100, true);

		String description = definition.describe();
		assertTrue(description.contains("D"));
		assertTrue(description.contains("100"));
		assertTrue(description.contains("true"));
	}

	@Test
	void describeShouldReturnQuestionMarkWhenMessageHasNoMsgType() {
		Message msg = new Message();
		StaticResponseDefinition definition = new StaticResponseDefinition(msg);

		String description = definition.describe();
		assertTrue(description.contains("?"));
	}

	@Test
	void singleArgConstructorShouldDefaultToZeroDelayAndNoDisconnect() {
		Message response = new Message();
		StaticResponseDefinition definition = new StaticResponseDefinition(response);
		assertEquals(0, definition.getDelayMillis());
		assertFalse(definition.shouldDisconnect());
	}
}
