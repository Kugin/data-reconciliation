# FileResourceLoader Auto-Detection

**Date:** 2026-04-26
**Status:** approved

## Motivation

`FileResourceLoader` requires explicit `identityField` and `checkFields` parameters, but `CheckEntry.wrap()` already supports automatic detection via `@CheckIdentity`/`@CheckField` annotations and the `CheckAdapter` interface. When both parameters are empty, the loader should fall back to auto-detection rather than the annotation-only path.

## Design

Single change in `FileResourceLoader.load()`:

```java
// Before:
return CheckEntry.wrap(list, identityField, checkFields);

// After:
if (CharSequenceUtil.isEmpty(identityField) && (checkFields == null || checkFields.isEmpty())) {
    return CheckEntry.wrap(list);     // adapter > annotation
}
return CheckEntry.wrap(list, identityField, checkFields);  // explicit path
```

### Behavior

| `identityField` | `checkFields` | Path |
|---|---|---|
| non-empty | non-empty | Explicit (existing) |
| non-empty | empty/null | Explicit (existing) |
| empty | non-empty | Explicit (existing) |
| empty | empty/null | **Auto-detect** (new) |

### Why two different `wrap` calls

- `CheckEntry.wrap(entity, null, null)` skips `CheckAdapter` and only checks annotations — because null `identityField` triggers annotation fallback, not adapter fallback
- `CheckEntry.wrap(entity)` checks `CheckAdapter` first, then annotations — the correct auto-detect order

## Scope

- File: `src/main/java/io/github/kugin/reconciliation/before/FileResourceLoader.java`
- Lines changed: ~4
- Backwards compatible: existing callers with explicit parameters unchanged
- New dependency: `CharSequenceUtil` already imported in `CheckEntry.java`, needs adding to `FileResourceLoader.java`
