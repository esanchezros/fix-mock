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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SequenceResponseDefinitionDetailTest {

	@Test
	void shouldReturnDelayFromFirstResponseBeforeAnyCall() {
		Message msg1 = new Message();
		msg1.getHeader().setString(35, "8");
		StaticResponseDefinition resp1 = new StaticResponseDefinition(msg1, 100, false);

		Message msg2 = new Message();
		msg2.getHeader().setString(35, "8");
		StaticResponseDefinition resp2 = new StaticResponseDefinition(msg2, 500, true);

		SequenceResponseDefinition seq = new SequenceResponseDefinition(List.of(resp1, resp2));

		assertEquals(100, seq.getDelayMillis());
	}

	@Test
	void shouldReturnDelayFromSecondResponseAfterAdvancing() throws Exception {
		Message msg1 = new Message();
		msg1.getHeader().setString(35, "8");
		StaticResponseDefinition resp1 = new StaticResponseDefinition(msg1, 100, false);

		Message msg2 = new Message();
		msg2.getHeader().setString(35, "8");
		StaticResponseDefinition resp2 = new StaticResponseDefinition(msg2, 500, true);

		SequenceResponseDefinition seq = new SequenceResponseDefinition(List.of(resp1, resp2));

		seq.generateResponse(null); // advance past first
		assertEquals(500, seq.getDelayMillis());
	}

	@Test
	void shouldReturnDisconnectFalseFromFirstResponse() {
		Message msg1 = new Message();
		msg1.getHeader().setString(35, "8");
		StaticResponseDefinition resp1 = new StaticResponseDefinition(msg1, 100, false);

		Message msg2 = new Message();
		msg2.getHeader().setString(35, "8");
		StaticResponseDefinition resp2 = new StaticResponseDefinition(msg2, 500, true);

		SequenceResponseDefinition seq = new SequenceResponseDefinition(List.of(resp1, resp2));

		assertFalse(seq.shouldDisconnect());
	}

	@Test
	void shouldReturnDisconnectTrueAfterAdvancingToSecondResponse() throws Exception {
		Message msg1 = new Message();
		msg1.getHeader().setString(35, "8");
		StaticResponseDefinition resp1 = new StaticResponseDefinition(msg1, 100, false);

		Message msg2 = new Message();
		msg2.getHeader().setString(35, "8");
		StaticResponseDefinition resp2 = new StaticResponseDefinition(msg2, 500, true);

		SequenceResponseDefinition seq = new SequenceResponseDefinition(List.of(resp1, resp2));

		seq.generateResponse(null); // advance past first
		assertTrue(seq.shouldDisconnect());
	}

	@Test
	void shouldReturnDelayFromLastResponseAfterExhaustion() throws Exception {
		Message msg1 = new Message();
		msg1.getHeader().setString(35, "8");
		StaticResponseDefinition resp1 = new StaticResponseDefinition(msg1, 100, false);

		Message msg2 = new Message();
		msg2.getHeader().setString(35, "8");
		StaticResponseDefinition resp2 = new StaticResponseDefinition(msg2, 500, true);

		SequenceResponseDefinition seq = new SequenceResponseDefinition(List.of(resp1, resp2));

		seq.generateResponse(null); // first -> second
		seq.generateResponse(null); // second -> clamped at second
		seq.generateResponse(null); // still second

		assertEquals(500, seq.getDelayMillis());
		assertTrue(seq.shouldDisconnect());
	}
}
