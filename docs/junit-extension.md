### JUnit 5 extension — FixMockExtension

Use the JUnit 5 extension to manage FixMock server lifecycle and options with minimal boilerplate.

#### Quickstart

```java
import io.allune.fixmock.junit.FixMockExtension;
import io.allune.fixmock.FixMockServerOptions;
import org.junit.jupiter.api.RegisterExtension;
import org.junit.jupiter.api.Test;

import static io.allune.fixmock.FixMock.*;

class OrderFlowTest {

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
  void executes_basic_flow() {
    fixMock.stubFor(
        FixMock.stubFor(FixMock.newOrderSingle()).willReturn(
            respondWithTemplate(() -> buildER()).copyFromRequestTags(11, 55)
        )
    );

    // ... run your client against BROKER:9880 ...

    fixMock.verify(
        FixMock.exactly(1),
        FixMock.executionReport() // ExecutionReport
    );
  }
}
```

#### Per-class vs per-test lifecycle

- Static `@RegisterExtension` creates a single server for all tests in the class.
- Use a non-static field to have a fresh server per test method.

#### Common patterns

- Switch to initiator role by supplying `FixMockServerOptions.builder().role(INITIATOR)...`.
- Enable dictionary-aware parsing with `useDataDictionary(true)`; omit `.dictionary(...)` to use bundled defaults by begin-string.

#### Troubleshooting

- If your client connects too quickly, use small waits or client-side retries; the extension starts the server before the test runs, but network timing can vary.
- Bind conflicts: adjust `acceptorPort` or let your client read the chosen port from configuration.
