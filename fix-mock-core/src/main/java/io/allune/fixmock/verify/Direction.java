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

/**
 * Indicates the FIX message direction (layer) within the QuickFIX/J Application callbacks.
 */
public enum Direction {
	/** Administrative messages such as Logon, Logout, Heartbeat, TestRequest, and SequenceReset. */
	ADMIN,
	/** Application-level business messages such as NewOrderSingle, ExecutionReport, and MarketDataRequest. */
	APP
}
