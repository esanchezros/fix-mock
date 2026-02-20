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
package io.allune.fixmock.server.http.dto;

import java.util.Map;

/**
 * Minimal pattern DTO for registering stubs via HTTP.
 */
public class PatternDTO {
	public enum Direction { ADMIN, APP, ANY }

	private Direction direction = Direction.ANY;
	private String msgType; // 35=
	private Map<Integer, String> headers; // exact match for now
	private Map<Integer, String> tags;	// exact match for now

	public Direction getDirection() { return direction; }
	public void setDirection(Direction direction) { this.direction = direction; }

	public String getMsgType() { return msgType; }
	public void setMsgType(String msgType) { this.msgType = msgType; }

	public Map<Integer, String> getHeaders() { return headers; }
	public void setHeaders(Map<Integer, String> headers) { this.headers = headers; }

	public Map<Integer, String> getTags() { return tags; }
	public void setTags(Map<Integer, String> tags) { this.tags = tags; }
}
