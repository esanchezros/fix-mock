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

import io.allune.fixmock.FixMock;
import io.allune.fixmock.FixMockServer;
import io.allune.fixmock.matching.FixMessagePatternBuilder;
import io.allune.fixmock.server.http.dto.VerifyRequestDTO;
import io.allune.fixmock.server.http.dto.VerifyResultDTO;
import io.allune.fixmock.server.http.mapping.DtoToDslMapper;
import io.allune.fixmock.verify.CountMatchingStrategy;
import io.allune.fixmock.verify.FixJournalEntry;
import io.allune.fixmock.verify.TimeWindow;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/__admin")
class VerifyController {

	private final ServerManager serverManager;

	VerifyController(ServerManager serverManager) {
		this.serverManager = serverManager;
	}

	@PostMapping(path = "/verify", consumes = "application/json", produces = "application/json")
	public ResponseEntity<VerifyResultDTO> verify(@RequestBody VerifyRequestDTO request) {
		if (request == null || request.getPattern() == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("pattern is required"));
		}
		FixMockServer server = serverManager.getServer();

		CountMatchingStrategy count = DtoToDslMapper.toCountStrategy(request.getCountType(), request.getCount());
		FixMessagePatternBuilder pattern = DtoToDslMapper.toPatternBuilder(request.getPattern());

		// Compute matches locally against the snapshot so we can provide actual count without throwing
		List<FixJournalEntry> snapshot = server.getJournalSnapshot();
		long cutoff = (request.getWithinMillis() != null && request.getWithinMillis() > 0)
				? Instant.now().minusMillis(request.getWithinMillis()).toEpochMilli()
				: Long.MIN_VALUE;
		long actual = snapshot.stream()
				.filter(e -> e.getTimestampMillis() >= cutoff)
				.filter(pattern::test)
				.count();

		boolean ok = count.matches(actual);
		VerifyResultDTO out = new VerifyResultDTO();
		out.setSuccess(ok);
		out.setCountType((request.getCountType() == null ? VerifyRequestDTO.CountType.EXACTLY : request.getCountType()).name());
		out.setExpected(request.getCount());
		out.setActual(actual);
		out.setWithinMillis(request.getWithinMillis());
		if (!ok) {
			String msg = String.format("Verification failed%s: expected %s but found %d. Pattern=%s",
					request.getWithinMillis() != null ? (" within "+ Duration.ofMillis(request.getWithinMillis())) : "",
					count.describe(), actual, pattern.describe());
			out.setMessage(msg);
			// Provide last 5 raw messages for debugging
			List<String> recent = snapshot.stream()
					.sorted(Comparator.comparingLong(FixJournalEntry::getTimestampMillis).reversed())
					.limit(5)
					.map(FixJournalEntry::getRaw)
					.collect(Collectors.toList());
			out.setRecentRaw(recent);
			return ResponseEntity.status(422).body(out);
		}
		return ResponseEntity.ok(out);
	}

	private static VerifyResultDTO error(String message) {
		VerifyResultDTO dto = new VerifyResultDTO();
		dto.setSuccess(false);
		dto.setMessage(message);
		return dto;
	}
}
