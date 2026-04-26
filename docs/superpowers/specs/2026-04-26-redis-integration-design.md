# Redis Integration Design

**Date**: 2026-04-26
**Status**: Draft

## Overview

Add Redis support to the data reconciliation framework: replace the in-memory `SimpleCache` with Redis-backed distributed executor state management, and introduce a `RedisResourceLoader` for loading reconciliation data from Redis.

## Scope

Two new classes, one new dependency:

- `RedisExecutorManager` — extends `AbstractExecutorManager`, replaces `FIFOCache` with Redisson `RBucket`/`RLock`
- `RedisResourceLoader<T>` — implements `ResourceLoader`, mirrors `FileResourceLoader` but reads from Redis
- Redisson 3.x as compile dependency

## Architecture

### RedisExecutorManager

Extends `AbstractExecutorManager`. Constructor accepts `RedissonClient` (user-owned, injected).

**Keys** (inherited from `AbstractExecutorManager`):
- State key: `day:check:<id>:<date>` → `RBucket<String>`, TTL 24h
- Lock key: `day:check:<id>:<date>_lock` → `RLock`

**Methods**:

| Method | Implementation |
|---|---|
| `isProcessing()` | `lock.isLocked()` |
| `isComplete()` | `getCurrentStatus() != null && "END".equals(getCurrentStatus())` |
| `setStatus(status)` | `RBucket.set(status.toString())`. On `BEFORE`: `lock.lock()`. On `END`: `lock.unlock()`. |
| `pauseByError()` | `lock.unlock()` (force-unlock, releases re-entrant guard) |
| `getCurrentStatus()` | `RBucket.get()` — returns null if key expired or never set |

**Differences from DefaultExecutorManager**:
- `RLock` is a true distributed lock, not a cache key check
- Status key has 24h TTL to prevent zombie keys
- `getCurrentStatus()` returns `null` on miss (vs `false` from cache)
- No `SimpleCache` / `FIFOCache` dependency

### RedisResourceLoader

Implements `ResourceLoader`. Mirrors `FileResourceLoader` structure.

**Fields**:
- `String redisKeyPattern` — pattern with `%s` for date, e.g. `"recon:data:%s"`
- `String identityField` — optional, for explicit field mapping
- `List<String> checkFields` — optional, for explicit field mapping
- `RedisEntityParser<T> redisEntityParser` — functional interface for per-line parsing
- `RedissonClient redissonClient` — injected

**`load(String date)` flow**:
1. `String.format(redisKeyPattern, date)` → Redis key
2. `RList<String>` from key, `readAll()` → `List<String>`
3. Map each line via `parser.parse(line)` → `List<T>`
4. `CheckEntry.wrap(entities, identityField, checkFields)` — auto-detect when both empty

**`RedisEntityParser<T>`** (inner `@FunctionalInterface`):
```java
T parse(String line);
```

## Data Flow

Pipeline unchanged. Only the storage/locking backend and data source change:

```
CheckExecutor.process(date)
  RedisExecutorManager.initDate(date)   // RLock + RBucket
  BeforeCheckProcessor
    RedisResourceLoader.load(date)       // RList read → parse → CheckEntry.wrap
  CheckProcessor.compare()               // unchanged
  AfterCheckProcessor
    RedisExecutorManager.setStatus(END)  // RBucket + RUnlock
```

## Error Handling

| Scenario | Behavior |
|---|---|
| Redis connection loss | Redisson internal retry; `load()` throws raw exception, caught by `CheckExecutor`, calls `pauseByError()` |
| RLock already held | `lock.lock()` with 0s wait — immediate rejection, `isProcessing()=true`, executor returns early |
| Status key TTL expired | `getCurrentStatus()` returns `null`, treated as not-yet-run |
| `pauseByError` force-unlock | `lock.forceUnlock()` — releases guard so next attempt can proceed |

## Dependencies

Add to `pom.xml`:
- `org.redisson:redisson` (3.x, compile)

Add to `pom.xml` (test):
- An embedded Redis library (test scope, exact artifact TBD during implementation)

## Testing

### RedisExecutorManagerTest

- `testNormalLifecycle` — `BEFORE` → `CHECK` → `AFTER` → `END`, verify each status
- `testReentrantBlocking` — two executors same id, second `isProcessing()=true`
- `testPauseByError` — error at `CHECK` stage, `pauseByError()` releases lock
- `testStatusTTL` — status key has TTL, `getCurrentStatus()` returns null after expiry

### RedisResourceLoaderTest

- `testLoadWithRList` — seed `RList` with test strings, verify parsed `CheckEntry` list
- `testLoadWithExplicitFields` — specify `identityField` and `checkFields`, verify `checkData`
- `testLoadAutoDetect` — both fields empty, verify auto-detection via adapter/annotations
- `testKeyPatternFormat` — verify `String.format(redisKeyPattern, date)` produces correct key

Test setup: `@Before` starts embedded Redis + creates `RedissonClient`; `@After` shuts down.
