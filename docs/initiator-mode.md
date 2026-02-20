### Initiator mode — quickstart and options

This guide shows how to run FixMock as either an acceptor (server) or an initiator (client) using the same API.

#### TL;DR

```java
// Acceptor (server) — your test app connects to FixMock
FixMockServer acceptor = new SimpleFixMockServer("FIX.4.4", "BROKER", "CLIENT", 9900);
acceptor.start();

// Initiator (client) — FixMock connects to your acceptor
FixMockServerOptions opts = FixMockServerOptions.builder()
    .role(FixMockServerOptions.Role.INITIATOR)
    .beginString("FIX.4.4")
    .senderCompId("CLIENT")
    .targetCompId("BROKER")
    .initiatorHost("127.0.0.1")
    .initiatorPort(9900)
    .build();
FixMockServer initiator = new SimpleFixMockServer(opts);
initiator.start();
```

#### When to use which

- Acceptor mode: you want to test your FIX initiator code. Your code dials into FixMock.
- Initiator mode: you want to test your FIX acceptor code. FixMock dials into your endpoint.

#### Common stubbing example (works in both roles)

```java
server.stubFor(
    FixMock.stubFor(FixMock.newOrderSingle())
        .willReturn(
            FixMock.respondWithTemplate(() -> new quickfix.fix44.ExecutionReport(
                new quickfix.field.OrderID("OID1"), new quickfix.field.ExecID("E1"),
                new quickfix.field.ExecType(quickfix.field.ExecType.NEW),
                new quickfix.field.OrdStatus(quickfix.field.OrdStatus.NEW),
                new quickfix.field.Side(quickfix.field.Side.BUY),
                new quickfix.field.LeavesQty(100), new quickfix.field.CumQty(0), new quickfix.field.AvgPx(0)
            )).copyFromRequestTags(11, 55) // ClOrdID, Symbol
        )
);
```

#### Options reference

- beginString: FIX version, e.g. `FIX.4.4`
- senderCompId / targetCompId: CompIDs for the session
- role: `ACCEPTOR` or `INITIATOR`
- acceptorPort: TCP port to bind in acceptor mode
- initiatorHost / initiatorPort: remote host/port to connect in initiator mode
- heartbeats: the server uses QuickFIX/J defaults unless customized in options
- data dictionary:
  - `options.useDataDictionary(true)`: enable dictionary-aware parsing
  - `options.dictionary(path)`: override with a custom dictionary. If not set, a bundled dictionary is chosen by `beginString` (FIX42, FIX44, FIX50SP2)

#### Troubleshooting

- Not connecting? Verify role, host/port, and CompIDs match your counterparty.
- Dictionary errors? Set `useDataDictionary(false)` or provide the correct dictionary path.
- Race on start-up? After `server.start()`, allow a short delay or wait for logon before sending app messages.

See also: realistic tests under `fix-mock-examples` (initiator and acceptor flows).
