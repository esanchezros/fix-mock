### FixMock Standalone Server

The `fix-mock-server` module provides a Spring Boot application that wraps a `FixMockServer` and exposes it via a REST API. This is useful for testing distributed systems where you need to manage FIX stubs and verifications over HTTP.

#### Quickstart

1. Build the project:
   ```bash
   mvn clean install
   ```
2. Run the server:
   ```bash
   java -jar fix-mock-server/target/fix-mock-server-0.2.0-SNAPSHOT-exec.jar
   ```
   The defaults are HTTP `9090` and FIX `9899`. Both accept `0` for ephemeral binding; `GET /ready` reports the selected FIX port and Spring logs the selected HTTP port.

#### Configuration

Configuration is managed via `application.properties` or environment variables (prefixed with `FIXMOCK_`).

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `9090` | HTTP REST API port |
| `fixmock.acceptorPort` | `9899` | FIX Acceptor port |
| `server.port=0` / `SERVER_PORT=0` | unset | Bind HTTP to an ephemeral port |
| `fixmock.beginString` | `FIX.4.4` | FIX version |
| `fixmock.senderCompId`| `BROKER` | Server's SenderCompID |
| `fixmock.targetCompId`| `CLIENT` | Server's TargetCompID |
| `fixmock.useDataDictionary` | `false` | Enable dictionary-aware parsing |

#### REST API Reference

The endpoints documented below are the stable public contract for the 0.2.x line. Additive fields and endpoints may be introduced compatibly; removals or semantic changes require a new major version and migration notes. Error shapes are not yet globally standardized (see release-readiness deferrals).

##### 1. Health Check
`GET /health` or `GET /ready`

Returns the status of the embedded FIX server.

**Example:**
```bash
curl http://localhost:9090/health
```

**Response:**
```json
{
  "status": "UP",
  "running": true,
  "fixPort": 49152
}
```

##### 2. Register Stubs
`POST /__admin/stubs`

Registers one or more stubs. v1 supports static `ExecutionReport` (35=8) responses.

**Example (Single Stub):**
```bash
curl -X POST http://localhost:9090/__admin/stubs \
  -H "Content-Type: application/json" \
  -d '{
    "pattern": { "direction": "APP", "msgType": "D" },
    "response": {
      "type": "STATIC_ER",
      "orderId": "OID-123",
      "execId": "E-456",
      "execType": "0",
      "ordStatus": "0",
      "side": "1",
      "leavesQty": 100,
      "cumQty": 0,
      "avgPx": 0
    }
  }'
```

**Example (Multiple Stubs):**
```bash
curl -X POST http://localhost:9090/__admin/stubs \
  -H "Content-Type: application/json" \
  -d '[
    {
      "pattern": { "msgType": "D" },
      "response": { "type": "STATIC_ER", "execType": "0", "ordStatus": "0" }
    },
    {
      "pattern": { "msgType": "F" },
      "response": { "type": "STATIC_ER", "execType": "4", "ordStatus": "4" }
    }
  ]'
```

##### 3. Verify Messages
`POST /__admin/verify`

Verifies that messages matching a pattern were received.

**Example (Success Case):**
```bash
curl -X POST http://localhost:9090/__admin/verify \
  -H "Content-Type: application/json" \
  -d '{
    "countType": "EXACTLY",
    "count": 1,
    "pattern": { "msgType": "D" }
  }'
```

**Example (With Time Window):**
```bash
curl -X POST http://localhost:9090/__admin/verify \
  -H "Content-Type: application/json" \
  -d '{
    "countType": "AT_LEAST",
    "count": 1,
    "withinMillis": 5000,
    "pattern": { "msgType": "D", "tags": { "55": "AAPL" } }
  }'
```

**Response (Success):** Returns `200 OK` with `success: true`.
**Response (Failure):** Returns `422 Unprocessable Entity` with details and recent raw messages.

##### 4. Retrieve Journal
`GET /__admin/requests`

Lists recorded messages with filtering and pagination.

**Example (List All):**
```bash
curl http://localhost:9090/__admin/requests
```

**Example (Filtered by MsgType and Session):**
```bash
curl "http://localhost:9090/__admin/requests?msgType=D&direction=APP&sessionId=FIX.4.4:BROKER->CLIENT&size=10"
```

**Query Parameters:**
- `direction`: `APP` or `ADMIN`
- `msgType`: e.g., `D`
- `sessionId`: e.g., `FIX.4.4:BROKER->CLIENT`
- `page`: default 0
- `size`: default 50

##### 5. Export Recordings
`GET /__admin/recordings`

Returns a list of stub mappings captured while in Recording Mode (`RecordMode.CAPTURE`). Returns an empty list if recording mode is `OFF`.

**Example:**
```bash
curl http://localhost:9090/__admin/recordings
```

##### 6. Reset Server
`DELETE /__admin/reset`

Clears all registered stubs and the message journal.

**Example:**
```bash
curl -X DELETE http://localhost:9090/__admin/reset
```

#### Docker

A minimal Dockerfile is available in the module root.
```bash
docker build -t fix-mock-server .
docker run -p 127.0.0.1::9090 -p 127.0.0.1::9899 fix-mock-server
docker port <container-id>
```
