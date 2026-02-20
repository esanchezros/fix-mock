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
package io.allune.fixmock.examples;

import io.allune.fixmock.FixMockServer;
import io.allune.fixmock.FixMockServerOptions;
import io.allune.fixmock.server.SimpleFixMockServer;
import io.allune.fixmock.stubbing.StubMapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.*;
import quickfix.field.*;
import quickfix.field.MsgType;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.NewOrderSingle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FixMockRecordingIT {

	private FixMockServer server;
	private Initiator initiator;

	@BeforeEach void setUp() { server = null; initiator = null; }
	@AfterEach void tearDown() throws Exception { if (initiator != null) initiator.stop(); if (server != null) server.stop(); }

	@Test
	void should_record_conversation_and_export_stubs() throws Exception {
		int port = 9950;
		
		// Start server with CAPTURE enabled
		FixMockServerOptions options = FixMockServerOptions.builder()
				.beginString("FIX.4.4")
				.senderCompId("BROKER")
				.targetCompId("CLIENT")
				.acceptorPort(port)
				.recordMode(FixMockServerOptions.RecordMode.CAPTURE)
				.build();
		
		server = new SimpleFixMockServer(options);
		server.start();

		// Stub a response so there's something to record as outbound
		ExecutionReport er = new ExecutionReport(
				new OrderID("OID1"), new ExecID("E1"),
				new ExecType(ExecType.NEW), new OrdStatus(OrdStatus.NEW),
				new Side(Side.BUY), new LeavesQty(100), new CumQty(0), new AvgPx(0)
		);
		server.stubFor(io.allune.fixmock.FixMock.when(io.allune.fixmock.FixMock.newOrderSingle())
				.willReturn(io.allune.fixmock.FixMock.respondWith(er)));

		// Initiator sends a NOS
		initiator = startInitiator("CLIENT", "BROKER", port, "AAPL");
		assertTrue(((SimpleFixMockServer) server).awaitLogon(java.time.Duration.ofSeconds(5)), "Should log on");
		Thread.sleep(1000);

		// Retrieve recorded stubs
		List<StubMapping> recorded = server.getRecordedStubs();

		// Find the NOS stub
		StubMapping stub = recorded.stream()
				.filter(s -> s.getRequestPattern().describe().contains("MsgType=D"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("NOS stub not recorded. Total recorded: " + recorded.size()));
		
		assertTrue(stub.getRequestPattern().describe().contains("tag[55]=equalTo(\"AAPL\")"));
		
		// Clean up and verify recording stopped if disabled? 
		// For now, simple success is enough.
	}

	private static Initiator startInitiator(String sender, String target, int port, String symbol) throws ConfigError, InterruptedException {
		SessionSettings settings = new SessionSettings();
		Properties defaults = new Properties();
		defaults.setProperty(SessionFactory.SETTING_CONNECTION_TYPE, "initiator");
		defaults.setProperty(Initiator.SETTING_RECONNECT_INTERVAL, "2");
		defaults.setProperty("UseDataDictionary", "N");
		defaults.setProperty("HeartBtInt", "30");
		defaults.setProperty("StartTime", "00:00:00");
		defaults.setProperty("EndTime", "23:59:59");
		settings.set(defaults);

		SessionID sessionID = new SessionID("FIX.4.4", sender, target);
		settings.setString(sessionID, SessionFactory.SETTING_CONNECTION_TYPE, "initiator");
		settings.setString(sessionID, SocketInitiator.SETTING_SOCKET_CONNECT_HOST, "127.0.0.1");
		settings.setString(sessionID, SocketInitiator.SETTING_SOCKET_CONNECT_PORT, Integer.toString(port));

		Application app = new Application() {
			@Override public void onCreate(SessionID sessionId) {}
			@Override public void onLogon(SessionID sessionId) {
				try {
					NewOrderSingle nos = new NewOrderSingle(
							new ClOrdID(UUID.randomUUID().toString()),
							new Side(Side.BUY),
							new TransactTime(LocalDateTime.now()),
							new OrdType(OrdType.MARKET)
					);
					nos.set(new Symbol(symbol));
					Session.sendToTarget(nos, sessionId);
				} catch (Exception ignored) {}
			}
			@Override public void onLogout(SessionID sessionId) {}
			@Override public void toAdmin(Message message, SessionID sessionId) {}
			@Override public void fromAdmin(Message message, SessionID sessionId) {}
			@Override public void toApp(Message message, SessionID sessionId) {}
			@Override public void fromApp(Message message, SessionID sessionId) {}
		};

		Initiator initiator = new SocketInitiator(app, new MemoryStoreFactory(), settings, new ScreenLogFactory(true, true, true), new DefaultMessageFactory());
		initiator.start();
		return initiator;
	}
}
