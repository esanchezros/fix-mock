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
package io.allune.fixmock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for a single FIX session within a FixMockServer.
 */
public final class SessionConfig {
	private final String beginString;
	private final String senderCompId;
	private final String senderSubId;
	private final String targetCompId;
	private final String targetSubId;
	private final FixMockServerOptions.Role role;
	private final Integer port;
	private final String host; // for INITIATOR role
	private final Map<String, String> settings;

	private SessionConfig(Builder b) {
		this.beginString = b.beginString;
		this.senderCompId = b.senderCompId;
		this.senderSubId = b.senderSubId;
		this.targetCompId = b.targetCompId;
		this.targetSubId = b.targetSubId;
		this.role = b.role;
		this.port = b.port;
		this.host = b.host;
		this.settings = Map.copyOf(b.settings);
	}

	public static Builder builder() { return new Builder(); }

	public String getBeginString() { return beginString; }
	public String getSenderCompId() { return senderCompId; }
	public String getSenderSubId() { return senderSubId; }
	public String getTargetCompId() { return targetCompId; }
	public String getTargetSubId() { return targetSubId; }
	public FixMockServerOptions.Role getRole() { return role; }
	public Integer getPort() { return port; }
	public String getHost() { return host; }
	public Map<String, String> getSettings() { return settings; }

	@Override
	public String toString() {
		return beginString + ":" + senderCompId + "->" + targetCompId + " (" + role + ")";
	}

	public static final class Builder {
		private String beginString;
		private String senderCompId;
		private String senderSubId;
		private String targetCompId;
		private String targetSubId;
		private FixMockServerOptions.Role role = FixMockServerOptions.Role.ACCEPTOR;
		private Integer port = 0;
		private String host;
		private final Map<String, String> settings = new LinkedHashMap<>();

		public Builder beginString(String bs) { this.beginString = bs; return this; }
		public Builder senderCompId(String s) { this.senderCompId = s; return this; }
		public Builder senderSubId(String s) { this.senderSubId = s; return this; }
		public Builder targetCompId(String t) { this.targetCompId = t; return this; }
		public Builder targetSubId(String t) { this.targetSubId = t; return this; }
		public Builder role(FixMockServerOptions.Role role) { this.role = role; return this; }
		public Builder port(int port) { this.port = port; return this; }
		public Builder host(String host) { this.host = host; return this; }
		public Builder setting(String key, String value) {
			this.settings.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
			return this;
		}
		public Builder settings(Map<String, String> settings) {
			Objects.requireNonNull(settings, "settings").forEach(this::setting);
			return this;
		}

		public SessionConfig build() {
			Objects.requireNonNull(beginString, "beginString");
			Objects.requireNonNull(senderCompId, "senderCompId");
			Objects.requireNonNull(targetCompId, "targetCompId");
			Objects.requireNonNull(role, "role");
			if (port == null || port < 0 || port > 65535) {
				throw new IllegalArgumentException("port must be between 0 and 65535");
			}
			if (role == FixMockServerOptions.Role.INITIATOR) {
				if (host == null || host.isBlank()) throw new IllegalArgumentException("host is required for INITIATOR role");
				if (port == 0) throw new IllegalArgumentException("port must be between 1 and 65535 for INITIATOR role");
			}
			return new SessionConfig(this);
		}
	}
}
