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
package io.allune.fixmock.server.http.mapping;

import io.allune.fixmock.FixMock;
import io.allune.fixmock.matching.FixMessagePatternBuilder;
import io.allune.fixmock.server.http.dto.PatternDTO;
import io.allune.fixmock.server.http.dto.ResponseDTO;
import io.allune.fixmock.server.http.dto.VerifyRequestDTO;
import io.allune.fixmock.server.http.dto.StubDTO;
import io.allune.fixmock.stubbing.ResponseDefinition;
import io.allune.fixmock.stubbing.StubMapping;
import quickfix.field.*;
import quickfix.fix44.ExecutionReport;

import java.util.Map;

/**
 * Maps HTTP DTOs to FixMock DSL (pattern + response).
 */
public final class DtoToDslMapper {
	private DtoToDslMapper() {}

	public static FixMessagePatternBuilder toPatternBuilder(PatternDTO dto) {
		FixMessagePatternBuilder b;
		if (dto == null) throw new IllegalArgumentException("pattern is required");
		if (dto.getDirection() == null || dto.getDirection() == PatternDTO.Direction.ANY) {
			b = FixMessagePatternBuilder.any();
		} else if (dto.getDirection() == PatternDTO.Direction.ADMIN) {
			b = FixMessagePatternBuilder.admin();
		} else {
			b = FixMessagePatternBuilder.app();
		}
		if (dto.getMsgType() != null) {
			b.messageType(dto.getMsgType());
		}
		if (dto.getHeaders() != null) {
			for (Map.Entry<Integer, String> e : dto.getHeaders().entrySet()) {
				b.withHeader(e.getKey(), FixMock.equalTo(e.getValue()));
			}
		}
		if (dto.getTags() != null) {
			for (Map.Entry<Integer, String> e : dto.getTags().entrySet()) {
				b.withTag(e.getKey(), FixMock.equalTo(e.getValue()));
			}
		}
		return b;
	}

	public static ResponseDefinition toResponse(ResponseDTO dto) {
		if (dto == null) throw new IllegalArgumentException("response is required");
		ResponseDTO.Type type = dto.getType() == null ? ResponseDTO.Type.STATIC_ER : dto.getType();
		switch (type) {
			case STATIC_ER:
			default:
				// Build a simple FIX 4.4 ExecutionReport
				ExecutionReport er = new ExecutionReport(
						new OrderID(nvl(dto.getOrderId(), "OID-1")),
						new ExecID(nvl(dto.getExecId(), "E1")),
						new ExecType(asChar(dto.getExecType(), ExecType.NEW)),
						new OrdStatus(asChar(dto.getOrdStatus(), OrdStatus.NEW)),
						new Side(asChar(dto.getSide(), Side.BUY)),
						new LeavesQty(dto.getLeavesQty()),
						new CumQty(dto.getCumQty()),
						new AvgPx(dto.getAvgPx())
				);
				return FixMock.respondWith(er);
		}
	}

	public static StubMapping toStubMapping(StubDTO dto) {
		FixMessagePatternBuilder pattern = toPatternBuilder(dto.getPattern());
		ResponseDefinition response = toResponse(dto.getResponse());
		return FixMock.stubFor(pattern).willReturn(response);
	}

	public static io.allune.fixmock.verify.CountMatchingStrategy toCountStrategy(VerifyRequestDTO.CountType type, int count) {
		VerifyRequestDTO.CountType t = (type == null ? VerifyRequestDTO.CountType.EXACTLY : type);
		return switch (t) {
			case EXACTLY -> FixMock.exactly(count);
			case AT_LEAST -> FixMock.atLeast(count);
			case AT_MOST -> FixMock.atMost(count);
			case MORE_THAN_OR_EXACTLY -> FixMock.moreThanOrExactly(count);
		};
	}

	private static String nvl(String s, String def) { return s == null || s.isBlank() ? def : s; }

	private static char asChar(String s, char def) {
		if (s == null || s.isEmpty()) return def;
		return s.charAt(0);
	}
}
