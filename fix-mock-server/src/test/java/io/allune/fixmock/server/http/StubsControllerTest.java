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
package io.allune.fixmock.server.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StubsControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void register_single_stub_returns_created() throws Exception {
		String payload = "{\n" +
				"  \"pattern\": { \"direction\": \"APP\", \"msgType\": \"D\" },\n" +
				"  \"response\": { \n" +
				"	\"type\": \"STATIC_ER\", \n" +
				"	\"orderId\": \"OID-IT\", \n" +
				"	\"execId\": \"E-IT\", \n" +
				"	\"execType\": \"0\", \n" +
				"	\"ordStatus\": \"0\", \n" +
				"	\"side\": \"1\", \n" +
				"	\"leavesQty\": 100, \n" +
				"	\"cumQty\": 0, \n" +
				"	\"avgPx\": 0 \n" +
				"  }\n" +
				"}";

		mockMvc.perform(post("/__admin/stubs")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.registered").value(1))
				.andExpect(jsonPath("$.summaries[0]").exists());
	}
}
