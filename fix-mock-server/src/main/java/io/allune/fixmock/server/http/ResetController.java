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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/__admin")
class ResetController {

	private final ServerManager serverManager;

	ResetController(ServerManager serverManager) {
		this.serverManager = serverManager;
	}

	@DeleteMapping("/reset")
	public ResponseEntity<Map<String, Object>> reset() {
		FixMockServer server = serverManager.getServer();
		int removedRequests = server.clearJournal();
		int removedStubs = server.clearStubs();
		Map<String, Object> body = new HashMap<>();
		body.put("removedRequests", removedRequests);
		body.put("removedStubs", removedStubs);
		return ResponseEntity.ok(body);
	}
}
