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
          .build()
  );

  @Test
  void executes_basic_flow() {
    fixMock.stubFor(
        when(newOrderSingle()).willReturn(
            respondWithTemplate(() -> buildER()).copyFromRequestTags(11, 55)
        )
    );

    // ... run your client against BROKER at `fixMock.port()` ...

    fixMock.verify(
        FixMock.exactly(1),
        FixMock.executionReport() // ExecutionReport
    );
  }
}
```

#### Per-class vs per-test lifecycle
The default is `PER_CLASS`. For an instance per method, construct the extension with `FixMockExtension.Lifecycle.PER_TEST`; a non-static field by itself does not change the explicit lifecycle. The extension also supports method-parameter injection of `FixMockServer` and `FixMockExtension`.


- Use `PER_CLASS` for one server per test class or `PER_TEST` for a fresh server per test method.

#### Common patterns

- Switch to initiator role by supplying `FixMockServerOptions.builder().role(INITIATOR)...`.
- Enable dictionary-aware parsing with `useDataDictionary(true)`; omit `.dictionary(...)` to use bundled defaults by begin-string.

#### Troubleshooting

- If your client connects too quickly, use `fixMock.awaitLogon(timeout)` or callback-driven client readiness; the extension starts the server before the test runs, but network timing can vary.
- Bind conflicts: use the default ephemeral port and configure the client from `fixMock.port()`.
