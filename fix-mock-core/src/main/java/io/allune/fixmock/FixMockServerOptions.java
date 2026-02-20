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

import io.allune.fixmock.stubbing.StubMapping;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration object for FixMockServer. Preferred over constructor overloads.
 */
public final class FixMockServerOptions {

	public enum Role { ACCEPTOR, INITIATOR }
	public enum RecordMode { OFF, CAPTURE, PLAYBACK }

	private final List<SessionConfig> sessions;
	private final List<StubMapping> stubs;
	private final RecordMode recordMode;

	// Common/QFJ settings
	private final boolean useDataDictionary;
	private final String dictionaryPath; // optional path or classpath resource
	private final int heartBtIntSeconds;
	private final boolean resetOnLogon;
	private final boolean resetOnDisconnect;
	private final boolean resetOnLogout;

	private FixMockServerOptions(Builder b) {
		this.sessions = new ArrayList<>(b.sessions);
		this.stubs = new ArrayList<>(b.stubs);
		this.recordMode = b.recordMode;
		this.useDataDictionary = b.useDataDictionary;
		this.dictionaryPath = b.dictionaryPath;
		this.heartBtIntSeconds = b.heartBtIntSeconds;
		this.resetOnLogon = b.resetOnLogon;
		this.resetOnDisconnect = b.resetOnDisconnect;
		this.resetOnLogout = b.resetOnLogout;
	}

	public static Builder builder() { return new Builder(); }

	public Builder toBuilder() { return new Builder(this); }

	public List<SessionConfig> getSessions() { return sessions; }
	public List<StubMapping> getStubs() { return stubs; }
	public RecordMode getRecordMode() { return recordMode; }
	
	// Compatibility getters for single-session use cases
	public Role getRole() { return sessions.get(0).getRole(); }
	public String getBeginString() { return sessions.get(0).getBeginString(); }
	public String getSenderCompId() { return sessions.get(0).getSenderCompId(); }
	public String getTargetCompId() { return sessions.get(0).getTargetCompId(); }
	public Integer getAcceptorPort() { return sessions.get(0).getPort(); }
	public String getInitiatorHost() { return sessions.get(0).getHost(); }
	public Integer getInitiatorPort() { return sessions.get(0).getPort(); }

	public boolean isUseDataDictionary() { return useDataDictionary; }
	public String getDictionaryPath() { return dictionaryPath; }
	public int getHeartBtIntSeconds() { return heartBtIntSeconds; }
	public boolean isResetOnLogon() { return resetOnLogon; }
	public boolean isResetOnDisconnect() { return resetOnDisconnect; }
	public boolean isResetOnLogout() { return resetOnLogout; }

	public static final class Builder {
		private final List<SessionConfig> sessions = new ArrayList<>();
		private final List<StubMapping> stubs = new ArrayList<>();
		private RecordMode recordMode = RecordMode.OFF;
		
		// Single session builder state for compatibility
		private Role role = Role.ACCEPTOR;
		private String beginString;
		private String senderCompId;
		private String targetCompId;
		private Integer acceptorPort;
		private String initiatorHost;
		private Integer initiatorPort;
		
		private boolean useDataDictionary = false;
		private String dictionaryPath;
		private int heartBtIntSeconds = 30;
		private boolean resetOnLogon = true;
		private boolean resetOnDisconnect = true;
		private boolean resetOnLogout = true;

		private Builder() {}

		private Builder(FixMockServerOptions base) {
			this.sessions.addAll(base.sessions);
			this.stubs.addAll(base.stubs);
			this.recordMode = base.recordMode;
			this.useDataDictionary = base.useDataDictionary;
			this.dictionaryPath = base.dictionaryPath;
			this.heartBtIntSeconds = base.heartBtIntSeconds;
			this.resetOnLogon = base.resetOnLogon;
			this.resetOnDisconnect = base.resetOnDisconnect;
			this.resetOnLogout = base.resetOnLogout;
		}

		public Builder recordMode(RecordMode mode) {
			this.recordMode = Objects.requireNonNull(mode);
			return this;
		}

		public Builder addSession(SessionConfig config) {
			this.sessions.add(Objects.requireNonNull(config));
			return this;
		}

		public Builder stubMappings(List<StubMapping> stubs) {
			this.stubs.addAll(Objects.requireNonNull(stubs));
			return this;
		}

		public Builder stubMapping(StubMapping stub) {
			this.stubs.add(Objects.requireNonNull(stub));
			return this;
		}

		public Builder role(Role role) { this.role = Objects.requireNonNull(role); return this; }
		public Builder beginString(String bs) { this.beginString = bs; return this; }
		public Builder senderCompId(String s) { this.senderCompId = s; return this; }
		public Builder targetCompId(String t) { this.targetCompId = t; return this; }
		public Builder acceptorPort(int port) { this.acceptorPort = port; return this; }
		public Builder initiatorHost(String host) { this.initiatorHost = host; return this; }
		public Builder initiatorPort(int port) { this.initiatorPort = port; return this; }
		public Builder useDataDictionary(boolean v) { this.useDataDictionary = v; return this; }
		public Builder dictionary(String path) { this.dictionaryPath = path; return this; }
		public Builder heartBtIntSeconds(int seconds) { this.heartBtIntSeconds = seconds; return this; }
		public Builder resetOnLogon(boolean v) { this.resetOnLogon = v; return this; }
		public Builder resetOnDisconnect(boolean v) { this.resetOnDisconnect = v; return this; }
		public Builder resetOnLogout(boolean v) { this.resetOnLogout = v; return this; }

		public FixMockServerOptions build() {
			if (sessions.isEmpty()) {
				// Compatibility mode: use builder fields to create a single session
				Objects.requireNonNull(role, "role");
				Objects.requireNonNull(beginString, "beginString");
				Objects.requireNonNull(senderCompId, "senderCompId");
				Objects.requireNonNull(targetCompId, "targetCompId");
				
				SessionConfig.Builder sb = SessionConfig.builder()
						.beginString(beginString)
						.senderCompId(senderCompId)
						.targetCompId(targetCompId)
						.role(role);
				
				switch (role) {
					case ACCEPTOR -> {
						if (acceptorPort == null) throw new IllegalArgumentException("acceptorPort is required for ACCEPTOR role");
						sb.port(acceptorPort);
					}
					case INITIATOR -> {
						if (initiatorHost == null || initiatorHost.isBlank()) throw new IllegalArgumentException("initiatorHost is required for INITIATOR role");
						if (initiatorPort == null) throw new IllegalArgumentException("initiatorPort is required for INITIATOR role");
						sb.host(initiatorHost).port(initiatorPort);
					}
				}
				sessions.add(sb.build());
			}
			return new FixMockServerOptions(this);
		}
	}
}
