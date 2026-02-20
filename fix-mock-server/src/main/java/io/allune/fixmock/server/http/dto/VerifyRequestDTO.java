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

/**
 * Request payload for POST /__admin/verify
 */
public class VerifyRequestDTO {
	public enum CountType { EXACTLY, AT_LEAST, AT_MOST, MORE_THAN_OR_EXACTLY }

	private CountType countType = CountType.EXACTLY;
	private int count = 1;
	private Long withinMillis; // optional time window
	private PatternDTO pattern;

	public CountType getCountType() { return countType; }
	public void setCountType(CountType countType) { this.countType = countType; }
	public int getCount() { return count; }
	public void setCount(int count) { this.count = count; }
	public Long getWithinMillis() { return withinMillis; }
	public void setWithinMillis(Long withinMillis) { this.withinMillis = withinMillis; }
	public PatternDTO getPattern() { return pattern; }
	public void setPattern(PatternDTO pattern) { this.pattern = pattern; }
}
