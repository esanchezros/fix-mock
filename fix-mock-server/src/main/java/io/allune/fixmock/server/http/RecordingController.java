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

import io.allune.fixmock.FixMockServer;
import io.allune.fixmock.server.http.dto.StubDTO;
import io.allune.fixmock.server.http.mapping.DslToDtoMapper;
import io.allune.fixmock.stubbing.StubMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/__admin")
class RecordingController {

	private final ServerManager serverManager;

	RecordingController(ServerManager serverManager) {
		this.serverManager = serverManager;
	}

	@GetMapping("/recordings")
	public ResponseEntity<Map<String, Object>> getRecordings() {
		FixMockServer server = serverManager.getServer();
		List<StubMapping> stubs = server.getRecordedStubs();

		List<StubDTO> items = new ArrayList<>();
		for (StubMapping stub : stubs) {
			items.add(DslToDtoMapper.toStubDto(stub));
		}

		Map<String, Object> body = new HashMap<>();
		body.put("stubs", items);
		return ResponseEntity.ok(body);
	}
}
