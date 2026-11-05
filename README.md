# FixMock

FixMock is a WireMock-inspired mocking library for FIX (Financial Information eXchange) protocol sessions using QuickFIX/J. It allows you to stub responses and verify received messages with a fluent Java DSL, making it ideal for integration testing FIX-enabled applications.

## Features

- **Fluent DSL**: Ergonomic API for stubbing and verification.
- **Embedded & Standalone**: Use it as a library in your tests or as a standalone server via REST.
- **Acceptor & Initiator Roles**: Mock both sides of a FIX connection.
- **Recording Mode**: Capture live traffic and auto-generate stub definitions.
- **Multi-session Support**: Manage multiple FIX sessions in a single instance with scoped verification.
- **Rich Matchers**: Match by MsgType, tags, regex, predicates, and human-readable field names.
- **Rich Verification Failures**: Detailed tag-level diffs and "closest match" identification for fast debugging.
- **Flexible Responses**: Static, templated (copy tags from request), sequences, delays, and negative testing (Rejects).
- **JUnit 5 Integration**: Manage server lifecycle automatically with `FixMockExtension`.

---

## Requirements
| Java | QuickFIX/J | Status |
|---|---|---|
| 17 or newer | 2.3.2 | Supported compatibility baseline |
| 17 or newer | 3.0.2 | Supported and preferred baseline |

Java 17 is the minimum. CI also exercises the current LTS on QuickFIX/J 3.0.2. Other 2.3.x or 3.0.x patch releases may work, but are not release-gate claims until added to the matrix.



---

## Maven Dependency

Add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>io.allune</groupId>
    <artifactId>fix-mock-core</artifactId>
    <version>0.2.0</version>
    <scope>test</scope>
</dependency>
```

For JUnit 5 support:

```xml
<dependency>
    <groupId>io.allune</groupId>
    <artifactId>fix-mock-junit</artifactId>
    <version>0.2.0</version>
    <scope>test</scope>
</dependency>
```

## QuickFIX/J and AssertJ selection

FixMock intentionally declares QuickFIX/J as optional. Add either `2.3.2` or `3.0.2` to the consuming test suite; this avoids changing an application engine version transitively. For the complete dictionaries, use `quickfixj-messages-all`.

```xml
<dependency>
    <groupId>org.quickfixj</groupId>
    <artifactId>quickfixj-core</artifactId>
    <version>3.0.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.quickfixj</groupId>
    <artifactId>quickfixj-messages-all</artifactId>
    <version>3.0.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.allune</groupId>
    <artifactId>assertj-quickfixj</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

FixMock returns defensive `quickfix.Message` values from `getReceivedMessages()` and `awaitMessage(...)`; AssertJ QuickFIX/J can assert those values directly without a production dependency from FixMock to AssertJ.

## Quick Start (JUnit 5)

Copy and paste this example to get started with an embedded acceptor mock in your test suite:

```java
import io.allune.fixmock.FixMockServerOptions;
import io.allune.fixmock.junit.FixMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import quickfix.fix44.ExecutionReport;
import quickfix.field.*;

import static io.allune.fixmock.FixMock.*;

class FixMockQuickStartTest {

    @RegisterExtension
    static FixMockExtension fixMock = new FixMockExtension(
        FixMockServerOptions.builder()
            .beginString("FIX.4.4")
            .senderCompId("BROKER")
            .targetCompId("CLIENT")
            .build()
    );

    @Test
    void shouldVerifyOrderReceipt() {
        // 1. Stub a response
        fixMock.stubFor(when(newOrderSingle())
            .willReturn(respondWith(new ExecutionReport(
                new OrderID("OID-1"), new ExecID("E-1"), new ExecType(ExecType.NEW),
                new OrdStatus(OrdStatus.NEW), new Side(Side.BUY), new LeavesQty(100),
                new CumQty(0), new AvgPx(0)
            ))));

        // 2. Run your application code that sends a NewOrderSingle to BROKER at `fixMock.port()`...

        // 3. Verify the message was received
        fixMock.verify(FixMock.exactly(1), FixMock.newOrderSingle()
            .withTagName("Symbol", equalTo("AAPL"))
            .withTagName("OrderQty", greaterThan(0)));
    }
}
```

The acceptor binds to an ephemeral port by default. Configure the application under test with `fixMock.port()` after the extension has started. For multiple sessions, use `fixMock.port(sessionId)`.

## Full Capability Showcase

This example demonstrates the library's advanced matching and response strategies in a single flow:

```java
@Test
void showcaseFullCapabilities() {
    // Advanced matching with Names, Predicates, and Groups
    fixMock.stubFor(when(marketDataSnapshotFullRefresh()
            .withTagName("Symbol", in("AAPL", "MSFT"))
            .withGroup("MDEntries", anyElement()
                .withTagName("MDEntryType", equalTo("0")) // Bid
                .withTagName("MDEntryPx", greaterThan(150.0))))
        .willReturn(sequence(
            respondWith(erNew).delay(Duration.ofMillis(100)),
            respondWith(erPartial),
            respondWith(erFill).disconnect()
        )));

    // Multi-session verification
    fixMock.session("FIX.4.4:BROKER->CLIENT").verify(FixMock.atLeast(1), 
        FixMock.newOrderSingle());

    // Verify within a time window
    fixMock.verify(FixMock.exactly(1), FixMock.within(Duration.ofSeconds(5)), 
        FixMock.orderCancelRequest()); // OrderCancelRequest
}
```

## Initiator Mode

Testing an acceptor application by having FixMock connect to it:

```java
FixMockServerOptions options = FixMockServerOptions.builder()
    .role(Role.INITIATOR)
    .beginString("FIX.4.4")
    .senderCompId("CLIENT")
    .targetCompId("BROKER")
    .initiatorHost("127.0.0.1")
    .initiatorPort(9899)
    .build();

SimpleFixMockServer server = new SimpleFixMockServer(options);
server.start();

// Send a message manually
server.send(new NewOrderSingle(...));

// Verify that the acceptor responded with an ExecutionReport
server.verify(FixMock.atLeast(1), FixMock.executionReport());
```

## Recording Mode

Bootstrap your mocks by capturing live traffic:

```java
FixMockServerOptions options = FixMockServerOptions.builder()
    .recordMode(RecordMode.CAPTURE)
    .build();

SimpleFixMockServer server = new SimpleFixMockServer(options);
server.start();

// ... live FIX traffic ...

// Export recorded stub mappings
List<StubMapping> stubs = server.getRecordedStubs();
```

## Stubbing Examples

### Templated Responses
Copy field values from the request into the response automatically:

```java
server.stubFor(when(newOrderSingle())
    .willReturn(respondWithTemplate(() -> new ExecutionReport(...))
        .copyFromRequestTags(11, 55) // ClOrdID, Symbol
    ));
```

### Response Sequences
Return different responses for subsequent matching requests:

```java
server.stubFor(when(newOrderSingle())
    .willReturn(sequence(
        respondWith(erNew),
        respondWith(erPartial),
        respondWith(erFill)
    )));
```

### Delays and Disconnects
Simulate network latency or connection issues:

```java
// Delay response by 500ms
server.stubFor(when(pattern).willReturn(respondWith(msg).delay(Duration.ofMillis(500))));

// Disconnect after sending response
server.stubFor(when(pattern).willReturn(respondWith(msg).disconnect()));
```

## Verification Examples

```java
// Verify count
server.verify(FixMock.exactly(2), FixMock.newOrderSingle());
server.verify(FixMock.atLeast(1), FixMock.executionReport());

// Verify within time window
server.verify(FixMock.exactly(1), FixMock.within(Duration.ofSeconds(5)), FixMock.newOrderSingle());

// Advanced matching
server.verify(FixMock.atLeast(1), FixMock.newOrderSingle()
    .withTagName("Side", sideIs(Side.BUY))
    .withTagName("OrderQty", greaterThan(100))
    .withTagName("ClOrdID", matching("^[A-Z0-9-]{10,36}$")));
```

## JUnit 5 Integration
`FixMockExtension` defaults to a per-class lifecycle and an ephemeral acceptor port. Pass `FixMockExtension.Lifecycle.PER_TEST` for isolation per test method. It waits for startup readiness and always tears down the server. Tests can also inject `FixMockServer` or `FixMockExtension` as method parameters.


```java
class MyFixTest {

    @RegisterExtension
    static FixMockExtension fixMock = new FixMockExtension(
        FixMockServerOptions.builder()
            .beginString("FIX.4.4")
            .senderCompId("BROKER")
            .targetCompId("CLIENT")
            .build()
    );

    @Test
    void testFlow() {
        fixMock.stubFor(when(newOrderSingle()).willReturn(respondWith(er)));

        // Connect your client to fixMock.port()...
        
        fixMock.verify(FixMock.exactly(1), FixMock.newOrderSingle());
    }
}
```

## Standalone REST Server

Run FixMock as a standalone service and control it via HTTP.

### Quick Start with Docker

Build and run the image locally from the project root:

```bash
# Build the project (run from project root)
mvn clean package -DskipTests

# Build Docker image
cd fix-mock-server
docker build -t allune/fix-mock-server .

# Run it
docker run -p 127.0.0.1::9090 -p 127.0.0.1::9899 allune/fix-mock-server
# Then run: docker port <container-id>
```

### Quick REST Examples

**Check health:**
```bash
curl http://localhost:9090/health
```

**Stub a response:**
```bash
curl -X POST http://localhost:9090/__admin/stubs \
  -H "Content-Type: application/json" \
  -d '{
    "pattern": { "msgType": "D" },
    "response": { "type": "STATIC_ER", "execType": "0", "ordStatus": "0" }
  }'
```

**Verify a message:**
```bash
curl -X POST http://localhost:9090/__admin/verify \
  -H "Content-Type: application/json" \
  -d '{
    "countType": "EXACTLY",
    "count": 1,
    "pattern": { "msgType": "D" }
  }'
```

### REST API Reference

For the full list of endpoints and query parameters, see the [Standalone Server Guide](docs/server.md).

- `POST /__admin/stubs`: Register a new stub.
- `GET /__admin/requests`: Retrieve the message journal.
- `POST /__admin/verify`: Verify received messages.
- `DELETE /__admin/reset`: Clear stubs and journal.
- `GET /__admin/recordings`: Export stubs captured in Recording Mode.
- `GET /health`: Server health status.

## Build and compatibility verification

`mvn --batch-mode verify` is canonical. Pull requests run the core, JUnit and REST transport smoke lanes on QuickFIX/J 2.3.2 and 3.0.2; main, scheduled and manual workflows run the complete example and ORF suites. The packaged consumer is tested outside the reactor, and the `api-compatibility` profile compares core against 0.1.0.


## Documentation

- [Detailed Matchers Guide](docs/matchers.md)
- [Response Strategies Cookbook](docs/response-strategies.md)
- [Initiator Mode Guide](docs/initiator-mode.md)
- [Recording Mode Guide](docs/recording-mode.md)
- [Multi-session Support](docs/multi-session.md)
- [JUnit 5 Extension Guide](docs/junit-extension.md)
- [REST API Reference](docs/server.md)

## License

Apache License, Version 2.0.
