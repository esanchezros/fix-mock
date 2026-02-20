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
import io.allune.fixmock.server.http.dto.JournalEntryDTO;
import io.allune.fixmock.server.http.dto.PagedRequestsDTO;
import io.allune.fixmock.verify.Direction;
import io.allune.fixmock.verify.FixJournalEntry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/__admin")
class RequestsController {

	private final ServerManager serverManager;

	RequestsController(ServerManager serverManager) {
		this.serverManager = serverManager;
	}

	@GetMapping(path = "/requests")
	public ResponseEntity<PagedRequestsDTO> list(
			@RequestParam(name = "direction", required = false) String direction,
			@RequestParam(name = "msgType", required = false) String msgType,
			@RequestParam(name = "sessionId", required = false) String sessionId,
			@RequestParam(name = "beginString", required = false) String beginString,
			@RequestParam(name = "since", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String since,
			@RequestParam(name = "until", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String until,
			@RequestParam(name = "page", required = false, defaultValue = "0") int page,
			@RequestParam(name = "size", required = false, defaultValue = "50") int size
	) {
		FixMockServer server = serverManager.getServer();
		List<FixJournalEntry> snapshot = server.getJournalSnapshot();

		Direction dir = parseDirection(direction);
		Long sinceMs = parseInstantMillis(since);
		Long untilMs = parseInstantMillis(until);

		List<FixJournalEntry> filtered = snapshot.stream()
				.filter(e -> dir == null || e.getDirection() == dir)
				.filter(e -> msgType == null || msgType.isEmpty() || msgType.equals(e.getMsgType()))
				.filter(e -> sessionId == null || sessionId.isEmpty() || sessionId.equals(e.getSessionId()))
				.filter(e -> beginString == null || beginString.isEmpty() || beginString.equals(e.getBeginString()))
				.filter(e -> sinceMs == null || e.getTimestampMillis() >= sinceMs)
				.filter(e -> untilMs == null || e.getTimestampMillis() <= untilMs)
				.collect(Collectors.toList());

		int from = Math.max(0, page * size);
		int to = Math.min(filtered.size(), from + size);
		List<JournalEntryDTO> items = (from >= to ? List.<FixJournalEntry>of() : filtered.subList(from, to))
				.stream().map(JournalEntryDTO::from).collect(Collectors.toList());

		PagedRequestsDTO dto = new PagedRequestsDTO(filtered.size(), page, size, items);
		return ResponseEntity.ok(dto);
	}

	private static Direction parseDirection(String raw) {
		if (raw == null || raw.isBlank()) return null;
		try {
			return Direction.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (Exception ignored) {
			return null;
		}
	}

	private static Long parseInstantMillis(String rawIso) {
		if (rawIso == null || rawIso.isBlank()) return null;
		try {
			return Instant.parse(rawIso).toEpochMilli();
		} catch (Exception ignored) {
			return null;
		}
	}
}
