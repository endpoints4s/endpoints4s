# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

endpoints4s is a Scala library for describing HTTP communication protocols. A single, abstract
description of an HTTP endpoint can be *interpreted* in multiple ways: as a server, as a client,
or as OpenAPI documentation. The library is cross-built for Scala 2.12, 2.13, and 3, and
cross-compiled to the JVM, Scala.js, and (for some modules) Scala Native.

## The algebra / interpreter pattern (the core idea)

This is the central abstraction; understanding it is essential to working in this repo.

- **Algebras** (`algebras/`, `json-schema/`) define *abstract* interfaces as traits with abstract
  type members and abstract methods (e.g. `Endpoints`, `Requests`, `Responses`, `Urls`,
  `JsonEntities`, `JsonSchemas`). Calling `endpoint(...)`, `get(...)`, `url`, etc. in an algebra
  produces values of *abstract* types (`Endpoint[A, B]`, `Request[A]`, `Url[A]`, ...). The algebra
  describes *what* an endpoint is without committing to *how* it is realized.

- **Interpreters** are concrete `trait` implementations that fix those abstract types and methods to
  a particular meaning:
  - **Server interpreters** (`pekko-http/server`, `http4s/server`, `akka-http/server`): an
    `Endpoint[A, B]` becomes a request handler `(A => B) => Route`.
  - **Client interpreters** (`sttp/client`, `http4s/client`, `xhr/client`, `fetch/client`,
    `pekko-http/client`, `akka-http/client`): an `Endpoint[A, B]` becomes a function `A => Result[B]`.
  - **Documentation interpreters** (`openapi/`): an `Endpoint[A, B]` becomes an OpenAPI `Item`.

  A user mixes the algebra trait and an interpreter trait together; the same endpoint definitions
  then "become" a server, a client, or docs depending on which interpreter is mixed in.

When adding a feature, the typical change touches **three layers in lockstep**: add the abstract
member to the algebra, then implement it in *every* interpreter (server, client, openapi), otherwise
those interpreters fail to compile. Tests for algebras live in `*-testkit` modules and are run
against each interpreter.

## Module layout

```
algebras/        Algebra interfaces (algebra, algebra-circe, algebra-playjson) + their testkits
json-schema/     JsonSchemas algebra + circe / playjson / generic derivation interpreters + utility
openapi/         Interpreter generating OpenAPI documentation
pekko-http/      Pekko-http based client and server interpreters
akka-http/       Akka-http based client and server interpreters
http4s/          http4s based client and server interpreters
sttp/            sttp based client interpreter
xhr/             Scala.js client interpreters based on XMLHttpRequest
fetch/           Scala.js client interpreters based on Fetch
stub-server/     HTTP server used by client interpreter tests (must be running for client tests)
documentation/   User manual (paradox) and runnable examples
sbt-assets/      Sbt plugin for asset handling
```

Build is split: the root `build.sbt` aggregates per-area `build.sbt` files (`algebras/build.sbt`,
`json-schema/build.sbt`, etc.). Shared settings and dependency versions live in
`project/EndpointsSettings.scala` (`commonSettings`, the `scala 2.12 to dotty` cross-version sets,
and all library version constants).

## Common commands

Run these from the `sbt` shell (start `sbt` from the repo root). Use `++ <version>` to select a
Scala version (`2.12`, `2.13`, `3`).

- Compile: `compile`
- Run all tests: `test`  (for a Scala version: `++ 2.13 test`)
- **Client interpreter tests require the stub server running**: prefix with
  `stub-server/bgRun`, e.g. `sbt stub-server/bgRun "++ 2.13 test"` (this is how CI runs it).
- Single module's tests: `<moduleId>/test`, e.g. `algebra-circe-jvm/test`. Cross-projects have
  per-platform suffixes (`-jvm`, `-js`, `-native`).
- Single test: `<moduleId>/testOnly *EndpointsDocs` (standard sbt `testOnly` filters).
- Format code (required by CI): `scalafmt` ; check only: `scalafmtCheck`.
- Check binary/source compatibility policy (required by CI): `+versionPolicyCheck`.
- Preview the documentation site: `manual/previewSite` (serves on http://localhost:4000).
- Run an example: `++ 2.13 example-quickstart-server/reStart`, then browse http://localhost:9000.
  Examples live under `documentation/examples/` (`example-cqrs`, `example-documented`,
  `example-quickstart-server`).

CI (`.github/workflows/ci.yml`) runs the full test matrix across 2.12 / 2.13 / 3, plus
`versionPolicyCheck scalafmtCheck`. `-Xfatal-warnings` is enabled when building in CI (see
`insideCI` in `EndpointsSettings.scala`), so warnings that pass locally can fail CI.

## Versioning and compatibility (important when changing public APIs)

Each module is published with its own version and its own compatibility guarantees.

- **Algebra modules** must stay backward **binary** compatible for as long as possible, because
  service APIs depend on them transitively and different services may use different endpoints4s
  versions. Interpreter modules may break compatibility more freely.
- The default intention is `Compatibility.BinaryAndSourceCompatible`
  (`versionPolicyIntention` in the root `build.sbt`). If a change must introduce a source or binary
  incompatibility and cannot be reworked, relax that setting on the affected module
  (`Compatibility.BinaryCompatible` or, for interpreters only, `Compatibility.None`) rather than
  forcing the change through.
- MiMa issue filters live in the root `build.sbt` (`mimaBinaryIssueFilters`).
- After each release the intention is reset back to `BinaryAndSourceCompatible` in every module.

See `CONTRIBUTING.md` for the full compatibility and release-process details.
