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
package io.allune.fixmock.verify;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class TimeWindowTest {

	@Test
	void withinShouldCreateWindowWithSpecifiedDuration() {
		Duration duration = Duration.ofSeconds(10);
		TimeWindow window = TimeWindow.within(duration);
		assertNotNull(window);
	}

	@Test
	void getDurationShouldReturnTheDurationPassed() {
		Duration duration = Duration.ofMinutes(5);
		TimeWindow window = TimeWindow.within(duration);
		assertEquals(duration, window.getDuration());
	}
}
