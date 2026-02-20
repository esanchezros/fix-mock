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

import io.allune.fixmock.matching.EqualToMatcher;
import io.allune.fixmock.matching.FixMessagePatternBuilder;
import io.allune.fixmock.server.http.dto.PatternDTO;
import io.allune.fixmock.server.http.dto.ResponseDTO;
import io.allune.fixmock.server.http.dto.StubDTO;
import io.allune.fixmock.stubbing.ResponseDefinition;
import io.allune.fixmock.stubbing.StaticResponseDefinition;
import io.allune.fixmock.stubbing.StubMapping;
import io.allune.fixmock.verify.Direction;
import quickfix.Message;
import quickfix.field.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps FixMock DSL objects back to HTTP DTOs for export.
 */
public final class DslToDtoMapper {
	private DslToDtoMapper() {}

	public static StubDTO toStubDto(StubMapping mapping) {
		StubDTO dto = new StubDTO();
		if (mapping.getRequestPattern() instanceof FixMessagePatternBuilder b) {
			dto.setPattern(toPatternDto(b));
		}
		if (mapping.getResponse() instanceof StaticResponseDefinition srd) {
			dto.setResponse(toResponseDto(srd));
		}
		return dto;
	}

	public static PatternDTO toPatternDto(FixMessagePatternBuilder builder) {
		PatternDTO dto = new PatternDTO();
		if (builder.getDirection() == Direction.APP) dto.setDirection(PatternDTO.Direction.APP);
		else if (builder.getDirection() == Direction.ADMIN) dto.setDirection(PatternDTO.Direction.ADMIN);
		else dto.setDirection(PatternDTO.Direction.ANY);

		dto.setMsgType(builder.getMsgType());

		if (!builder.getHeaderMatchers().isEmpty()) {
			Map<Integer, String> headers = new HashMap<>();
			for (FixMessagePatternBuilder.TagMatcher tm : builder.getHeaderMatchers()) {
				if (tm.getTag() != -1 && tm.getMatcher() instanceof EqualToMatcher em) {
					headers.put(tm.getTag(), extractValue(em));
				}
			}
			if (!headers.isEmpty()) dto.setHeaders(headers);
		}

		if (!builder.getTagMatchers().isEmpty()) {
			Map<Integer, String> tags = new HashMap<>();
			for (FixMessagePatternBuilder.TagMatcher tm : builder.getTagMatchers()) {
				if (tm.getTag() != -1 && tm.getMatcher() instanceof EqualToMatcher em) {
					tags.put(tm.getTag(), extractValue(em));
				}
			}
			if (!tags.isEmpty()) dto.setTags(tags);
		}

		return dto;
	}

	public static ResponseDTO toResponseDto(StaticResponseDefinition srd) {
		ResponseDTO dto = new ResponseDTO();
		dto.setType(ResponseDTO.Type.STATIC_ER);
		Message msg = srd.getResponse();
		if (msg != null) {
			try {
				if (msg.isSetField(OrderID.FIELD)) dto.setOrderId(msg.getString(OrderID.FIELD));
				if (msg.isSetField(ExecID.FIELD)) dto.setExecId(msg.getString(ExecID.FIELD));
				if (msg.isSetField(ExecType.FIELD)) dto.setExecType(msg.getString(ExecType.FIELD));
				if (msg.isSetField(OrdStatus.FIELD)) dto.setOrdStatus(msg.getString(OrdStatus.FIELD));
				if (msg.isSetField(Side.FIELD)) dto.setSide(msg.getString(Side.FIELD));
				if (msg.isSetField(LeavesQty.FIELD)) dto.setLeavesQty(msg.getDouble(LeavesQty.FIELD));
				if (msg.isSetField(CumQty.FIELD)) dto.setCumQty(msg.getDouble(CumQty.FIELD));
				if (msg.isSetField(AvgPx.FIELD)) dto.setAvgPx(msg.getDouble(AvgPx.FIELD));
			} catch (Exception ignored) {}
		}
		return dto;
	}

	private static String extractValue(EqualToMatcher em) {
		// EqualToMatcher.describe() returns "equalTo(\"value\")"
		String desc = em.describe();
		if (desc.startsWith("equalTo(\"") && desc.endsWith("\")")) {
			return desc.substring(9, desc.length() - 2);
		}
		return "";
	}
}
