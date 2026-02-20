### Matchers guide — predicates, conveniences, and repeating groups

This guide summarizes the value matchers available in FixMock, how to use them in patterns, and tips for repeating groups. It also notes current behavior when FIX dictionaries are enabled vs disabled.

#### Value matchers (predicates)

Numeric/range-aware (best‑effort):

```java
// Numeric when possible (BigDecimal), lexical fallback otherwise
greaterThan(100)
lessThan("250.5")
between(10, 20)
between("2024-01-01", "2024-12-31") // lexical inclusive
```

String matchers:

```java
equalTo("ABC-123")
matching("^[A-Z0-9\\-]{10,36}$")
containing("ABC")
in("A", "B", "C")
present()
absent()
anyOf(equalTo("A"), equalTo("B"))
allOf(containing("A"), matching("^A.*"))
not(equalTo("BAD"))
```

Use them with headers or body tags:

```java
FixMock.appMessageRequestedFor()
  .messageType("D")
  .withHeader(49, equalTo("CLIENT"))         // SenderCompID
  .withHeader(56, equalTo("BROKER"))         // TargetCompID
  .withTag(55, equalTo("AAPL"))              // Symbol
  .withTag(38, greaterThan(0));               // OrderQty > 0
```

#### Convenience matchers for common fields

```java
// Equivalent to equalTo("1") or equalTo("2") for char semantics
sideIs(quickfix.field.Side.BUY)    // or sideIs('1')
sideIs(quickfix.field.Side.SELL)   // or sideIs('2')

ordTypeIs(quickfix.field.OrdType.MARKET)  // or ordTypeIs('1')
execTypeIs(quickfix.field.ExecType.NEW)   // or execTypeIs('0')
```

Example:

```java
FixMock.appMessageRequestedFor()
  .messageType("8")                 // ExecutionReport
  .withTag(150, FixMock.execTypeIs('0'))     // ExecType=New
  .withTag(54, FixMock.sideIs(Side.BUY));    // Side=Buy
```

#### Repeating groups — current usage and tips

FixMock supports matching repeating groups using a selector for either any element or a specific index. Example below uses Parties `NoPartyIDs(453)` and checks `PartyID(448)`:

```java
FixMock.appMessageRequestedFor()
  .messageType("D")
  .withGroup(453, FixMock.anyElement()
      .withTag(448, equalTo("CLIENT-XYZ"))
  );
```

Index-based selection:

```java
FixMock.appMessageRequestedFor()
  .messageType("W") // MarketDataSnapshot
  .withGroup(268, FixMock.elementAt(0)
      .withTag(269, equalTo("0"))    // MDEntryType=Bid
      .withTag(270, greaterThan(100)) // MDEntryPx > 100
  );
```

Notes and limitations:
- For best results, enable dictionary-aware parsing on the server so group boundaries are resolved reliably when verifying:
  - Embedded: pass `FixMockServerOptions.builder().useDataDictionary(true)` (bundled FIX44/FIX42/FIX50SP2 are auto‑selected by `beginString`).
  - Standalone server: set `fixmock.useDataDictionary=true` (or provide a specific `fixmock.dictionaryPath`).
- When dictionary is disabled, direct tag checks still work (e.g., `.withTag(270, greaterThan(100))`), but group selectors may be limited depending on message structure.
- FIXT 1.1 transports: header parsing works with `FIXT11.xml`; application dictionaries vary by `ApplVerID`. Current matching uses session begin‑string to load a reasonable default and falls back gracefully if not available.

#### Names vs numbers

FixMock supports referring to fields and groups by their name (e.g., `Symbol`, `ClOrdID`, `Parties`) instead of just tag numbers. This is resolved using common aliases or the FIX dictionary for the active version.

```java
FixMock.appMessageRequestedFor()
  .messageType("D")
  .withHeaderName("SenderCompID", equalTo("CLIENT"))
  .withTagName("Symbol", equalTo("AAPL"))
  .withGroup("Parties", anyElement()
      .withTagName("PartyID", equalTo("CLIENT-XYZ"))
  );
```

#### Tag name resolution strategy

Names are resolved in order:

1. **Common aliases** - Built-in map of frequently used fields (always works, no dictionary needed)
2. **FIX Data Dictionary** - Loaded based on BeginString (FIX.4.2, FIX.4.4, FIX.5.0SP2)

**Common aliases recognized without dictionary:**

| Category | Fields |
|----------|--------|
| Header | SenderCompID (49), TargetCompID (56), BeginString (8), MsgType (35) |
| Order | ClOrdID (11), OrigClOrdID (41), Symbol (55), Side (54), OrdType (40), Price (44), OrderQty (38) |
| Execution | ExecType (150), OrdStatus (39) |
| Parties | Parties/NoPartyIDs (453), PartyID (448), PartyIDSource (447), PartyRole (452) |
| Market Data | MDEntries/NoMDEntries (268), MDEntryType (269), MDEntryPx (270), MDEntrySize (271) |

For custom fields or less common names, enable dictionary support on the server with `useDataDictionary(true)`.

#### Example snippets

At least one `NewOrderSingle` for AAPL with a sane `ClOrdID` format:

```java
server.verify(
  FixMock.moreThanOrExactly(1),
  FixMock.newOrderSingle()
    .withTag(55, equalTo("AAPL"))
    .withTag(11, matching("^[A-Z0-9\\-]{10,36}$"))
);
```

Market data with numeric predicates:

```java
server.verify(
  FixMock.atLeast(1),
  FixMock.marketDataSnapshotFullRefresh()
    .withTag(270, greaterThan(100))
    .withTag(271, between(10, 1000))
);
```

See also:
- `fix-mock-examples` integration tests for Parties and MDEntries
- `docs/response-strategies.md` for response templating, sequences, delays, disconnects, and rejects
