### Multi-session support

FixMock allows you to run multiple FIX sessions within a single `FixMockServer` instance. This is useful for testing applications that connect to multiple counterparties or require multiple sessions.

#### Configuring multiple sessions

You can use `FixMockServerOptions` to add multiple sessions using `SessionConfig`:

```java
FixMockServerOptions options = FixMockServerOptions.builder()
    .addSession(SessionConfig.builder()
        .beginString("FIX.4.4")
        .senderCompId("BROKER1")
        .targetCompId("CLIENT1")
        .port(9921)
        .role(Role.ACCEPTOR)
        .build())
    .addSession(SessionConfig.builder()
        .beginString("FIX.4.4")
        .senderCompId("BROKER2")
        .targetCompId("CLIENT2")
        .port(9922)
        .role(Role.ACCEPTOR)
        .build())
    .build();

FixMockServer server = new SimpleFixMockServer(options);
server.start();
```

#### Stubbing and Verification

Stubs and journals are shared across all sessions. You can target specific sessions by using `senderCompId`, `targetCompId`, or other session-specific tags in your patterns:

```java
import static io.allune.fixmock.FixMock.*;

// Stub for session 1
server.stubFor(when(newOrderSingle().senderCompId("CLIENT1"))
    .willReturn(respondWith(er1)));

// Verify message from session 2
server.verify(FixMock.exactly(1), FixMock.newOrderSingle()
    .senderCompId("CLIENT2"));
```

#### Initiator role

You can also mix acceptor and initiator roles in the same server instance:

```java
FixMockServerOptions options = FixMockServerOptions.builder()
    .addSession(SessionConfig.builder()
        .beginString("FIX.4.4")
        .senderCompId("BROKER1")
        .targetCompId("CLIENT1")
        .port(9921)
        .role(FixMockServerOptions.Role.ACCEPTOR)
        .build())
    .addSession(SessionConfig.builder()
        .beginString("FIX.4.4")
        .senderCompId("MOCK_CLIENT")
        .targetCompId("REAL_BROKER")
        .host("127.0.0.1")
        .port(9880)
        .role(FixMockServerOptions.Role.INITIATOR)
        .build())
    .build();
```

#### Scoped Verification

When dealing with many sessions, you can use the `session(String)` helper to focus your verification on a specific session. This automatically applies a `sessionId` filter to your pattern:

```java
// Focus on a specific session by ID
server.session("FIX.4.4:BROKER1->CLIENT1")
    .verify(FixMock.exactly(1), FixMock.newOrderSingle());

server.session("FIX.4.4:BROKER2->CLIENT2")
    .verify(FixMock.exactly(1), FixMock.newOrderSingle());
```

#### Sending messages

When using multiple initiators, you can target a specific session for sending:

```java
SessionID sessionID = new SessionID("FIX.4.4", "MOCK_CLIENT", "REAL_BROKER");
server.send(new NewOrderSingle(...), sessionID);
```
