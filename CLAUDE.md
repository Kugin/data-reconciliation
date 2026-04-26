# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Prerequisites

- JDK 21 (Homebrew: `/opt/homebrew/opt/openjdk@21`)
- `~/.m2/toolchains.xml` must point to JDK 21:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>21</version>
      <vendor>openjdk</vendor>
    </provides>
    <configuration>
      <jdkHome>/opt/homebrew/opt/openjdk@21</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

## Build / Test

```bash
# Build the project
mvn compile

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ExecutorTest

# Run a single test method
mvn test -Dtest=ExecutorTest#testExecutor
```

## Architecture

This is a generic data reconciliation (对账) framework for comparing data between two arbitrary data sources, identifying differences, and optionally auto-syncing corrections. Java 21, Maven, with Lombok and Hutool.

**Pipeline**: The `CheckExecutor` chains three processors in sequence:
1. **`BeforeCheckProcessor`** — Loads source/target data (`ResourceLoader.load(date)` returns `List<CheckEntry>`), runs pre-checks (`CheckPre`). The default impl (`DefaultBeforeCheckProcessor`) wraps a `ResourceReader` which provides separate `ResourceLoader` instances for source and target data sources.
2. **`CheckProcessor`** — Compares source and target `Map<String, CheckEntry>` by key. Produces `CheckResult` with diffs categorized as `SOURCE_MORE`, `TARGET_MORE`, or `DIFFERENT`. The default impl (`DefaultCheckProcessor`) delegates to the template method `compareMap()` in `AbstractCheckProcessor`.
3. **`AfterCheckProcessor`** — Auto-syncs diffs via `CheckSync` (per-diff-type callbacks: `syncSourceMore`, `syncTargetMore`, `syncDifferent`), then runs `CheckAfter.doAfter()`. Default sync behavior (when `CheckSync` is null): all diffs pass through as `SYNC`.

**Key domain types**:
- `CheckEntry` — Wraps an entity for comparison with a `key` (String) and `checkData` (Map of field → value). Created via `CheckEntry.wrap(entity)` which auto-detects `@CheckIdentity`/`@CheckField` annotations or delegates to `CheckAdapter` interface if the entity implements it. Adapter takes priority over annotations.
- `CheckConfig` — Builder-style config for an executor. At minimum needs `id` or `name`, and a way to load data (either `resourceReader` or `srcLoader`+`targetLoader`). All processor overrides are optional.
- `CheckResult` — Contains `diffDetails` (flat `List<CheckUnit>`), `diffDetailsMap` (grouped by `CheckStateEnum`), and separate `sourceDiffMap`/`targetDiffMap` for entries only on one side.
- `CheckUnit` — Per-entity diff: `key`, `state` (enum), `sync` (enum), `differentFields` (Map of field name → `FieldkUnit` with source/target values).
- `CheckContext` — Carries everything through the pipeline: source/target lists and maps, result, executor manager, stopwatch.

**Executor lifecycle**: `ExecutorManager` prevents re-entrant execution of the same id+date. Two implementations:
- `DefaultExecutorManager` — in-memory via `FIFOCache` singleton (`SimpleCache`)
- `RedisExecutorManager` — distributed via Redisson `RLock`/`RBucket`, for multi-node deployments

Status progresses: `BEFORE` → `CHECK` → `AFTER` → `END` (or `ERROR` on exception).

**Two ways to define what gets compared**:
1. Annotations — `@CheckIdentity` on the identity field, `@CheckField(order=…)` on fields to compare. `CheckEntry.wrap()` uses reflection.
2. `CheckAdapter` interface — entity implements `getKey()` and `getCheckData()`. This takes priority over annotations.

**Data loading**:
- `FileResourceLoader` — Reads data from files (path format string + date), parses lines via a `FileEntityParser` function. Supports auto-detect of wrapper strategy when `identityField` and `checkFields` are empty.
- `RedisResourceLoader` — Reads data from Redis `RList` via Redisson. Same pattern as `FileResourceLoader`: key pattern + parser + auto-detect.

**Redis integration** (`org.redisson:redisson:3.27.2`): `RedisExecutorManager` + `RedisResourceLoader`. Tests use `com.github.codemonstur:embedded-redis` (no external Redis needed).
