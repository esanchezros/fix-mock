### Recording Mode

Recording mode allows you to capture live FIX traffic and automatically generate stub definitions for later playback. This is particularly useful for bootstrapping tests from existing environments.

#### Enabling Recording Mode

You can enable recording mode via `FixMockServerOptions`:

```java
FixMockServerOptions options = FixMockServerOptions.builder()
    .beginString("FIX.4.4")
    .senderCompId("BROKER")
    .targetCompId("CLIENT")
    .recordMode(RecordMode.CAPTURE) // Enable capture
    .build();

SimpleFixMockServer server = new SimpleFixMockServer(options);
server.start();
```

#### Capturing Conversations

When `RecordMode.CAPTURE` is enabled, FixMock monitors incoming requests and outbound responses. It pairs them together to create `StubMapping` instances.

Heuristics are used to generate identification patterns for requests, focusing on `MsgType` and common correlation tags like `ClOrdID`, `Symbol`, or `ExecID`.

#### Exporting Recorded Stubs

You can retrieve the recorded stubs programmatically or via the REST API.

**Java API:**

```java
List<StubMapping> recorded = server.getRecordedStubs();
for (StubMapping stub : recorded) {
    System.out.println("Recorded Pattern: " + stub.getRequestPattern().describe());
}
```

**REST API:**

```bash
curl http://localhost:9090/__admin/recordings
```

#### Usage Tips

- **Bootstrap Tests**: Run your application against FixMock in recording mode to capture realistic interactions, then copy the resulting stub mappings into your unit test suite.
- **Dynamic Identification**: The recording engine identifies which tags in the request are likely to be keys (like `ClOrdID`) and includes them in the generated pattern.
- **Admin Messages**: Currently, recording focuses on application-level messages. Admin messages (like Logons or Heartbeats) are captured in the journal but may not be automatically converted to stubs unless they involve non-standard behavior.
