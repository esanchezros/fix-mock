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

- **Java 17** or higher
- **QuickFIX/J 2.3.1** (included as transitive dependency)

---

## Maven Dependency

Add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>io.allune</groupId>
    <artifactId>fix-mock-core</artifactId>
    <version>0.2.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

For JUnit 5 support:

```xml
<dependency>
    <groupId>io.allune</groupId>
    <artifactId>fix-mock-junit</artifactId>
    <version>0.2.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

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
            .acceptorPort(9899)
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

        // 2. Run your application code that sends a NewOrderSingle to BROKER:9899...

        // 3. Verify the message was received
        fixMock.verify(FixMock.exactly(1), FixMock.newOrderSingle()
            .withTagName("Symbol", equalTo("AAPL"))
            .withTagName("OrderQty", greaterThan(0)));
    }
}
```

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

```java
class MyFixTest {

    @RegisterExtension
    static FixMockExtension fixMock = new FixMockExtension(
        FixMockServerOptions.builder()
            .beginString("FIX.4.4")
            .senderCompId("BROKER")
            .targetCompId("CLIENT")
            .acceptorPort(9880)
            .build()
    );

    @Test
    void testFlow() {
        fixMock.stubFor(when(newOrderSingle()).willReturn(respondWith(er)));
        
        // Connect your client to 9880...
        
        fixMock.verify(FixMock.exactly(1), FixMock.newOrderSingle());
    }
}
```

## Standalone REST Server

Run FixMock as a standalone service and control it via HTTP.

### Quick Start with Docker

```bash
docker run -p 9090:9090 -p 9899:9899 allune/fix-mock-server
```

To build the Docker image locally:

```bash
# Build the project first
mvn clean package -DskipTests

# Build Docker image
cd fix-mock-server
docker build -t fix-mock-server .

# Run it
docker run -p 9090:9090 -p 9899:9899 fix-mock-server
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
