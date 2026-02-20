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
class VerifyControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void verify_returns_success_when_conditions_met() throws Exception {
		// Initially 0 matches
		String payload = "{\n" +
				"  \"countType\": \"EXACTLY\",\n" +
				"  \"count\": 0,\n" +
				"  \"pattern\": { \"msgType\": \"D\" }\n" +
				"}";

		mockMvc.perform(post("/__admin/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.actual").value(0));
	}

	@Test
	void verify_returns_422_when_conditions_not_met() throws Exception {
		// Expecting 1 but having 0
		String payload = "{\n" +
				"  \"countType\": \"EXACTLY\",\n" +
				"  \"count\": 1,\n" +
				"  \"pattern\": { \"msgType\": \"D\" }\n" +
				"}";

		mockMvc.perform(post("/__admin/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").exists());
	}
}
