### Response strategies — cookbook

FixMock supports several response styles you can mix to create realistic flows.

#### Static responses

```java
import static io.allune.fixmock.FixMock.*;

server.stubFor(when(newOrderSingle())
    .willReturn(respondWith(new quickfix.fix44.ExecutionReport(
      new quickfix.field.OrderID("OID"), new quickfix.field.ExecID("E1"),
      new quickfix.field.ExecType(quickfix.field.ExecType.NEW),
      new quickfix.field.OrdStatus(quickfix.field.OrdStatus.NEW),
      new quickfix.field.Side(quickfix.field.Side.BUY),
      new quickfix.field.LeavesQty(100), new quickfix.field.CumQty(0), new quickfix.field.AvgPx(0)
    )))
);
```

#### Templated responses (copy fields from request)

```java
server.stubFor(when(newOrderSingle())
    .willReturn(
      respondWithTemplate(() -> new quickfix.fix44.ExecutionReport(
        new quickfix.field.OrderID("OID-TPL"), new quickfix.field.ExecID("E1"),
        new quickfix.field.ExecType(quickfix.field.ExecType.NEW),
        new quickfix.field.OrdStatus(quickfix.field.OrdStatus.NEW),
        new quickfix.field.Side(quickfix.field.Side.BUY),
        new quickfix.field.LeavesQty(100), new quickfix.field.CumQty(0), new quickfix.field.AvgPx(0)
      )).copyFromRequestTags(11, 55) // ClOrdID, Symbol
    )
);
```

#### Sequences (X then Y ...)

```java
ResponseDefinition seq = sequence(
  respondWith(erNew),
  respondWith(erPartial),
  respondWith(erFill)
);
server.stubFor(when(newOrderSingle()).willReturn(seq));
```

See example tests: `FixMockSequenceIT`, `FixMockOrderLifecycleIT`.

#### Delays

```java
server.stubFor(when(newOrderSingle())
    .willReturn(
      respondWithTemplate(() -> buildER()).delay(java.time.Duration.ofMillis(400))
    )
);
```

See example: `FixMockResponseStrategiesIT#delayed_response_waits_at_least_configured_time`.

#### Disconnects

```java
server.stubFor(when(newOrderSingle())
    .willReturn(respondWith(msg).disconnect())
);
```

#### Rejects (negative testing)

```java
server.stubFor(when(newOrderSingle())
    .willReturn(respondWith(new quickfix.fix44.BusinessMessageReject()))
);
```

See example tests: `FixMockNegativeIT`, `FixMockResponseStrategiesIT`.
