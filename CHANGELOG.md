# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Ephemeral FIX acceptor ports by default, with selected ports exposed through embedded and JUnit APIs.
- Per-session QuickFIX/J setting injection, readiness/logon/logout/message waits, reset, disconnect, graceful and forced shutdown.
- JUnit `PER_CLASS` and `PER_TEST` lifecycles plus parameter injection.
- Peer-side FIX 4.4 transport tests in both initiator/acceptor directions using AssertJ QuickFIX/J 1.0.0.
- CI lanes for Java 17 and 25 with QuickFIX/J 2.3.2 and 3.0.2.
- A standalone packaged-artifact consumer fixture that runs the initiator-to-peer transport smoke against both QuickFIX/J lines.

### Changed

- QuickFIX/J dependencies are optional so applications select their supported engine line.
- Recorded state uses concurrent collections and returns immutable snapshots or defensive `Message` values.
- Delayed stub responses use a managed scheduler rather than blocking QuickFIX/J callback threads.
- Dependency convergence and forked-test timeouts are enforced by the canonical `mvn --batch-mode verify` build.
- Core line coverage is held at 80%, and the `api-compatibility` profile checks public binary and source compatibility against 0.1.0.

### Migration

- Acceptors now default to port `0`; read the selected port with `FixMockServer.getPort()` or `FixMockExtension.port()`. Set an explicit port only when required.
- Consumers must declare `quickfixj-core` and the required message module, normally `quickfixj-messages-all`, in test scope.
- Java 17 remains the minimum. Supported engine baselines are QuickFIX/J 2.3.2 and 3.0.2.

### Release checklist

- Run clean verification on both QuickFIX/J lines and inspect Surefire/Failsafe reports.
- Build with the `release` profile and inspect binary, source, Javadoc, POM and signatures.
- Verify the staged artifacts and dependencies resolve using Maven Central only.
- Run the packaged-consumer gate; run the Spring Boot starter interoperability gate when that deferred fixture lands.
- Obtain explicit maintainer approval before any version change, tag, push, Central upload/publication or GitHub release.


## [0.1.0] - 2026-02-19

### Added

- **Core Library** (`fix-mock-core`)
  - Fluent DSL for stubbing FIX message responses
  - Fluent DSL for verifying received FIX messages
  - Support for both Acceptor and Initiator roles
  - Rich matcher support: `equalTo`, `matching` (regex), `greaterThan`, `lessThan`, `between`, `in`, `containing`, `present`, `absent`, `anyOf`, `allOf`, `not`
  - Convenience matchers: `sideIs()`, `ordTypeIs()`, `execTypeIs()`
  - Human-readable field names via `withTagName()` (Symbol, ClOrdID, etc.)
  - Repeating group matching with `withGroup()`, `anyElement()`, `elementAt()`
  - Response strategies: static, templated (copy tags from request), sequences
  - Response modifiers: delays, disconnects
  - Scenario state management for stateful stubbing
  - Recording mode for capturing live traffic and generating stubs
  - Multi-session support with scoped verification
  - Time window verification with `within(Duration)`
  - Rich verification failure messages with closest match identification
  - Message journal for debugging

- **JUnit 5 Extension** (`fix-mock-junit`)
  - `FixMockExtension` for automatic server lifecycle management
  - `@RegisterExtension` support

- **Standalone Server** (`fix-mock-server`)
  - REST API for remote control of FixMock
  - Endpoints: `/health`, `/__admin/stubs`, `/__admin/verify`, `/__admin/requests`, `/__admin/recordings`, `/__admin/reset`
  - Docker support with provided Dockerfile
  - Spring Boot 3.2.5 based

- **Documentation**
  - Comprehensive README with quick start examples
  - Detailed matchers guide
  - Response strategies cookbook
  - Initiator mode guide
  - Recording mode guide
  - Multi-session support guide
  - JUnit extension guide

### Technical Details

- Java 17+ required
- Built on QuickFIX/J 2.3.1
- FIX 4.4 message support (convenience factories)
- Apache License 2.0

[Unreleased]: https://github.com/esanchezros/fix-mock/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/esanchezros/fix-mock/releases/tag/v0.1.0
