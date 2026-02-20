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
package io.allune.fixmock.server.http;

import io.allune.fixmock.FixMockServer;
import io.allune.fixmock.FixMockServerOptions;
import io.allune.fixmock.server.SimpleFixMockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
class ServerManager {
	private static final Logger log = LoggerFactory.getLogger(ServerManager.class);

	private final FixMockServerProperties props;
	private volatile FixMockServer server;

	ServerManager(FixMockServerProperties props) {
		this.props = props;
	}

	@PostConstruct
	void start() {
		try {
			FixMockServerOptions.Builder b = FixMockServerOptions.builder()
					.role(FixMockServerOptions.Role.ACCEPTOR)
					.beginString(props.getBeginString())
					.senderCompId(props.getSenderCompId())
					.targetCompId(props.getTargetCompId())
					.acceptorPort(props.getAcceptorPort())
					.heartBtIntSeconds(props.getHeartBtIntSeconds())
					.resetOnLogon(props.isResetOnLogon())
					.resetOnDisconnect(props.isResetOnDisconnect())
					.resetOnLogout(props.isResetOnLogout())
					.recordMode(FixMockServerOptions.RecordMode.valueOf(props.getRecordMode().toUpperCase()))
					.useDataDictionary(props.isUseDataDictionary());
			if (props.getDictionaryPath() != null && !props.getDictionaryPath().isBlank()) {
				b.dictionary(props.getDictionaryPath());
			}
			server = new SimpleFixMockServer(b.build());
			server.start();
			log.info("FixMock server started: beginString={}, {}->{}, port={}",
					props.getBeginString(), props.getSenderCompId(), props.getTargetCompId(), props.getAcceptorPort());
		} catch (Exception e) {
			log.error("Failed to start FixMock server", e);
			throw new IllegalStateException("Failed to start FixMock server", e);
		}
	}

	@PreDestroy
	void stop() {
		FixMockServer s = server;
		if (s != null) {
			try {
				s.stop();
				log.info("FixMock server stopped");
			} catch (Exception e) {
				log.warn("Error stopping FixMock server", e);
			}
		}
	}

	boolean isRunning() {
		FixMockServer s = server;
		return s != null && s.isRunning();
	}

	void stubFor(io.allune.fixmock.stubbing.StubMapping mapping) {
		FixMockServer s = server;
		if (s == null) throw new IllegalStateException("Server not started");
		s.stubFor(mapping);
	}

	FixMockServer getServer() {
		return server;
	}
}
