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
 * Minimal response DTOs. v1 supports a simple static FIX44 ExecutionReport.
 */
public class ResponseDTO {
	public enum Type { STATIC_ER }

	private Type type = Type.STATIC_ER;

	// Fields for a minimal ExecutionReport (FIX 4.4)
	private String orderId;   // 37
	private String execId;	// 17
	private String execType;  // 150 (char)
	private String ordStatus; // 39 (char)
	private String side;	  // 54 (char)
	private double leavesQty; // 151
	private double cumQty;	// 14
	private double avgPx;	 // 6

	public Type getType() { return type; }
	public void setType(Type type) { this.type = type; }

	public String getOrderId() { return orderId; }
	public void setOrderId(String orderId) { this.orderId = orderId; }

	public String getExecId() { return execId; }
	public void setExecId(String execId) { this.execId = execId; }

	public String getExecType() { return execType; }
	public void setExecType(String execType) { this.execType = execType; }

	public String getOrdStatus() { return ordStatus; }
	public void setOrdStatus(String ordStatus) { this.ordStatus = ordStatus; }

	public String getSide() { return side; }
	public void setSide(String side) { this.side = side; }

	public double getLeavesQty() { return leavesQty; }
	public void setLeavesQty(double leavesQty) { this.leavesQty = leavesQty; }

	public double getCumQty() { return cumQty; }
	public void setCumQty(double cumQty) { this.cumQty = cumQty; }

	public double getAvgPx() { return avgPx; }
	public void setAvgPx(double avgPx) { this.avgPx = avgPx; }
}
