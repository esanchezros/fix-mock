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

import java.util.List;

/**
 * Response payload for POST /__admin/verify
 */
public class VerifyResultDTO {
	private boolean success;
	private String countType;
	private int expected;
	private long actual;
	private Long withinMillis;
	private String message;
	private List<String> recentRaw; // compact recent entries, raw strings or brief summaries

	public boolean isSuccess() { return success; }
	public void setSuccess(boolean success) { this.success = success; }
	public String getCountType() { return countType; }
	public void setCountType(String countType) { this.countType = countType; }
	public int getExpected() { return expected; }
	public void setExpected(int expected) { this.expected = expected; }
	public long getActual() { return actual; }
	public void setActual(long actual) { this.actual = actual; }
	public Long getWithinMillis() { return withinMillis; }
	public void setWithinMillis(Long withinMillis) { this.withinMillis = withinMillis; }
	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }
	public List<String> getRecentRaw() { return recentRaw; }
	public void setRecentRaw(List<String> recentRaw) { this.recentRaw = recentRaw; }
}
