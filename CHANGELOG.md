# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
