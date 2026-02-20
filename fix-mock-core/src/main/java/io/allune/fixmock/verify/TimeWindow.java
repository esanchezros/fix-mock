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

import java.time.Duration;

/**
 * Constrains verification to messages received within a specific time window.
 * Used with {@link io.allune.fixmock.FixMockServer#verify(CountMatchingStrategy, TimeWindow, FixMessagePattern)}
 * to limit the scope of message matching to recent messages.
 *
 * <pre>
 * server.verify(exactly(1), within(Duration.ofSeconds(5)), newOrderSingle());
 * </pre>
 */
public final class TimeWindow {

	private final Duration duration;

	private TimeWindow(Duration duration) {
		this.duration = duration;
	}

	/**
	 * Creates a time window of the specified duration, measured backwards from the current time.
	 *
	 * @param duration the lookback duration
	 * @return a new TimeWindow instance
	 */
	public static TimeWindow within(Duration duration) {
		return new TimeWindow(duration);
	}

	/**
	 * Returns the duration of this time window.
	 *
	 * @return the lookback duration
	 */
	public Duration getDuration() {
		return duration;
	}
}
