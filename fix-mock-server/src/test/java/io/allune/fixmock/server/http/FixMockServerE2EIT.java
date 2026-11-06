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
import io.allune.fixmock.server.http.dto.PatternDTO;
import io.allune.fixmock.server.http.dto.ResponseDTO;
import io.allune.fixmock.server.http.dto.StubDTO;
import io.allune.fixmock.server.http.dto.VerifyRequestDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.NewOrderSingle;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "fixmock.acceptor-port=0")
class FixMockServerE2EIT {

	@LocalServerPort
	private int restPort;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private ServerManager serverManager;

	private Initiator initiator;

	@BeforeEach
	void setUp() {
		initiator = null;
	}

	@AfterEach
	void tearDown() {
		if (initiator != null) initiator.stop();
		restTemplate.delete("/__admin/reset");
	}

	@Test
	void end_to_end_rest_and_fix_flow() throws Exception {
		// 1. Register a stub via REST: NOS -> ER (NEW)
		StubDTO stub = new StubDTO();
		PatternDTO pattern = new PatternDTO();
		pattern.setDirection(PatternDTO.Direction.APP);
		pattern.setMsgType("D");
		stub.setPattern(pattern);

		ResponseDTO response = new ResponseDTO();
		response.setType(ResponseDTO.Type.STATIC_ER);
		response.setOrderId("E2E-OID");
		response.setExecId("E2E-EX1");
		response.setExecType("0"); // New
		response.setOrdStatus("0"); // New
		stub.setResponse(response);

		ResponseEntity<Object> regResponse = restTemplate.postForEntity("/__admin/stubs", stub, Object.class);
		assertEquals(HttpStatus.CREATED, regResponse.getStatusCode());

		// 2. Send real FIX message to the server's FIX port
		int fixPort = serverManager.boundPort();
		CapturingApp app = new CapturingApp();
		initiator = startInitiator(app, "CLIENT", "BROKER", fixPort);

		Message er = app.awaitAppMessage(5, TimeUnit.SECONDS);
		assertNotNull(er, "Expected ExecutionReport from server");
		assertEquals("E2E-OID", er.getString(OrderID.FIELD));
		assertEquals("E2E-EX1", er.getString(ExecID.FIELD));

		// 3. Verify via REST
		VerifyRequestDTO verifyRequest = new VerifyRequestDTO();
		verifyRequest.setCount(1);
		verifyRequest.setCountType(VerifyRequestDTO.CountType.EXACTLY);
		verifyRequest.setPattern(pattern);

		ResponseEntity<Object> verResponse = restTemplate.postForEntity("/__admin/verify", verifyRequest, Object.class);
		assertEquals(HttpStatus.OK, verResponse.getStatusCode());

		// 4. Check journal via REST
		ResponseEntity<Object> reqResponse = restTemplate.getForEntity("/__admin/requests?msgType=D", Object.class);
		assertEquals(HttpStatus.OK, reqResponse.getStatusCode());
		// Simple check that items array is not empty
		String body = reqResponse.getBody().toString();
		assertTrue(body.contains("total=1") || body.contains("total: 1") || body.contains("\"total\":1"), "Expected 1 request in journal");
	}

	private Initiator startInitiator(Application app, String sender, String target, int port) throws ConfigError, InterruptedException {
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

		MessageStoreFactory storeFactory = new MemoryStoreFactory();
		LogFactory logFactory = new ScreenLogFactory(true, true, true);
		MessageFactory messageFactory = new DefaultMessageFactory();
		Initiator initiator = new SocketInitiator(app, storeFactory, settings, logFactory, messageFactory);
		initiator.start();
		return initiator;
	}

	static class CapturingApp extends MessageCracker implements Application {
		private final CountDownLatch appLatch = new CountDownLatch(1);
		private volatile Message lastAppMessage;

		Message awaitAppMessage(long time, TimeUnit unit) throws InterruptedException {
			appLatch.await(time, unit);
			return lastAppMessage;
		}

		@Override public void onCreate(SessionID sessionId) {}
		@Override public void onLogon(SessionID sessionId) {
			try {
				NewOrderSingle nos = new NewOrderSingle(
						new ClOrdID(UUID.randomUUID().toString()),
						new Side(Side.BUY),
						new TransactTime(LocalDateTime.now()),
						new OrdType(OrdType.MARKET)
				);
				nos.set(new Symbol("AAPL"));
				Session.sendToTarget(nos, sessionId);
			} catch (Exception ignored) {}
		}
		@Override public void onLogout(SessionID sessionId) {}
		@Override public void toAdmin(Message message, SessionID sessionId) {}
		@Override public void fromAdmin(Message message, SessionID sessionId) {}
		@Override public void toApp(Message message, SessionID sessionId) {}
		@Override public void fromApp(Message message, SessionID sessionId) {
			lastAppMessage = message;
			appLatch.countDown();
		}
	}
}
