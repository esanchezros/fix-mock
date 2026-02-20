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

import io.allune.fixmock.server.http.dto.StubDTO;
import io.allune.fixmock.server.http.mapping.DtoToDslMapper;
import io.allune.fixmock.stubbing.StubMapping;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/__admin")
class StubsController {

	private final ServerManager serverManager;

	StubsController(ServerManager serverManager) {
		this.serverManager = serverManager;
	}

	@PostMapping(path = "/stubs", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> registerStubs(@RequestBody(required = true) Object body) {
		List<StubDTO> items = coerceToList(body);
		int registered = 0;
		List<String> summaries = new ArrayList<>();
		for (StubDTO dto : items) {
			StubMapping mapping = DtoToDslMapper.toStubMapping(dto);
			serverManager.stubFor(mapping);
			registered++;
			String mt = dto.getPattern() != null ? dto.getPattern().getMsgType() : null;
			String dir = dto.getPattern() != null && dto.getPattern().getDirection()!=null ? dto.getPattern().getDirection().name() : "ANY";
			summaries.add("pattern[msgType=" + (mt==null?"*":mt) + ",dir=" + dir + "]");
		}
		Map<String, Object> out = new HashMap<>();
		out.put("registered", registered);
		out.put("summaries", summaries);
		return ResponseEntity.status(201).body(out);
	}

	@SuppressWarnings("unchecked")
	private static List<StubDTO> coerceToList(Object body) {
		if (body instanceof List) {
			return (List<StubDTO>) body;
		}
		if (body instanceof Map) {
			// Single object
			StubDTO single = mapTo(body, StubDTO.class);
			List<StubDTO> list = new ArrayList<>();
			list.add(single);
			return list;
		}
		throw new IllegalArgumentException("Expected JSON object or array");
	}

	// Rely on Jackson to convert map structures into the DTO class
	private static <T> T mapTo(Object src, Class<T> type) {
		return new com.fasterxml.jackson.databind.ObjectMapper().convertValue(src, type);
	}
}
