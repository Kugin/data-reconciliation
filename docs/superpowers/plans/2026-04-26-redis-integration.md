# Redis Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Redis-backed executor state management and Redis data source loading to the reconciliation framework.

**Architecture:** Two new classes — `RedisExecutorManager` extends `AbstractExecutorManager` and replaces `FIFOCache` with Redisson `RBucket`/`RLock`; `RedisResourceLoader<T>` implements `ResourceLoader` mirroring `FileResourceLoader` but reading from Redis `RList`. RedissonClient is injected by the caller.

**Tech Stack:** Java 21, Redisson 3.x, JUnit 4, embedded-redis (test)

**Spec:** `docs/superpowers/specs/2026-04-26-redis-integration-design.md`

---

### Task 1: Add Redisson and embedded Redis dependencies

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add Redisson and embedded-redis dependency to pom.xml**

Add to `<dependencies>` before the Junit dependency:

```xml
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson</artifactId>
            <version>3.27.2</version>
        </dependency>
        <dependency>
            <groupId>it.ozimov</groupId>
            <artifactId>embedded-redis</artifactId>
            <version>0.7.3</version>
            <scope>test</scope>
        </dependency>
```

The Junit dependency also needs `<scope>test</scope>` — fix it while we're here. Replace:

```xml
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>${junit.version}</version>
        </dependency>
```

with:

```xml
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: Verify dependencies resolve**

Run: `mvn compile`
Expected: BUILD SUCCESS, redisson jar on classpath

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: add Redisson and embedded-redis dependencies"
```

---

### Task 2: Write RedisExecutorManager tests

**Files:**
- Create: `src/test/java/io/github/kugin/reconciliation/executor/RedisExecutorManagerTest.java`

- [ ] **Step 1: Create test class with embedded Redis setup**

```java
package io.github.kugin.reconciliation.executor;

import io.github.kugin.reconciliation.enums.ExecutorStatusEnum;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import redis.embedded.RedisServer;

public class RedisExecutorManagerTest {

    private RedisServer redisServer;
    private RedissonClient redissonClient;

    @Before
    public void setUp() {
        redisServer = RedisServer.builder()
                .port(63790)
                .setting("maxmemory 128M")
                .build();
        redisServer.start();
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:63790");
        redissonClient = Redisson.create(config);
    }

    @After
    public void tearDown() {
        redissonClient.shutdown();
        redisServer.stop();
    }

    @Test
    public void testNormalLifecycle() {
        RedisExecutorManager manager = new RedisExecutorManager("test-lifecycle", redissonClient);
        manager.initDate("20260426");

        String executorKey = manager.getExecutorKey();
        Assert.assertEquals("day:check:test-lifecycle:20260426", executorKey);

        manager.setStatus(ExecutorStatusEnum.BEFORE);
        Assert.assertEquals("BEFORE", manager.getCurrentStatus());
        Assert.assertTrue(manager.isProcessing());

        manager.setStatus(ExecutorStatusEnum.CHECK);
        Assert.assertEquals("CHECK", manager.getCurrentStatus());
        Assert.assertTrue(manager.isProcessing());

        manager.setStatus(ExecutorStatusEnum.AFTER);
        Assert.assertEquals("AFTER", manager.getCurrentStatus());
        Assert.assertTrue(manager.isProcessing());

        manager.setStatus(ExecutorStatusEnum.END);
        Assert.assertEquals("END", manager.getCurrentStatus());
        Assert.assertFalse(manager.isProcessing());
        Assert.assertTrue(manager.isComplete());
    }

    @Test
    public void testReentrantBlocking() {
        RedisExecutorManager manager1 = new RedisExecutorManager("test-reentrant", redissonClient);
        manager1.initDate("20260426");
        manager1.setStatus(ExecutorStatusEnum.BEFORE);
        Assert.assertTrue(manager1.isProcessing());

        RedisExecutorManager manager2 = new RedisExecutorManager("test-reentrant", redissonClient);
        manager2.initDate("20260426");
        Assert.assertTrue(manager2.isProcessing());
    }

    @Test
    public void testPauseByError() {
        RedisExecutorManager manager = new RedisExecutorManager("test-error", redissonClient);
        manager.initDate("20260426");
        manager.setStatus(ExecutorStatusEnum.BEFORE);
        Assert.assertTrue(manager.isProcessing());

        manager.pauseByError();
        Assert.assertFalse(manager.isProcessing());

        manager.setStatus(ExecutorStatusEnum.ERROR);
        Assert.assertEquals("ERROR", manager.getCurrentStatus());
    }

    @Test
    public void testNotCompleteWhenNoStatus() {
        RedisExecutorManager manager = new RedisExecutorManager("test-nostatus", redissonClient);
        manager.initDate("20260426");
        Assert.assertNull(manager.getCurrentStatus());
        Assert.assertFalse(manager.isComplete());
    }
}
```

- [ ] **Step 2: Run test to verify it fails (class doesn't exist yet)**

Run: `mvn test -Dtest=RedisExecutorManagerTest#testNormalLifecycle`
Expected: COMPILATION ERROR — `RedisExecutorManager` does not exist

- [ ] **Step 3: Commit**

```bash
git add src/test/java/io/github/kugin/reconciliation/executor/RedisExecutorManagerTest.java
git commit -m "test: add RedisExecutorManager tests (red, no impl yet)"
```

---

### Task 3: Implement RedisExecutorManager

**Files:**
- Create: `src/main/java/io/github/kugin/reconciliation/executor/RedisExecutorManager.java`

- [ ] **Step 1: Write RedisExecutorManager**

```java
package io.github.kugin.reconciliation.executor;

import io.github.kugin.reconciliation.enums.ExecutorStatusEnum;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

public class RedisExecutorManager extends AbstractExecutorManager {

    private final RedissonClient redissonClient;

    public RedisExecutorManager(String id, RedissonClient redissonClient) {
        super(id);
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean isProcessing() {
        return getLock().isLocked();
    }

    @Override
    public boolean isComplete() {
        String status = getCurrentStatus();
        return status != null && ExecutorStatusEnum.END.toString().equals(status);
    }

    @Override
    public void setStatus(ExecutorStatusEnum status) {
        if (ExecutorStatusEnum.BEFORE.equals(status)) {
            boolean acquired = getLock().tryLock();
            if (!acquired) {
                return;
            }
        }
        if (ExecutorStatusEnum.END.equals(status)) {
            RLock lock = getLock();
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        if (ExecutorStatusEnum.ERROR.equals(status)) {
            RLock lock = getLock();
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        getBucket().set(status.toString(), 24, TimeUnit.HOURS);
    }

    @Override
    public void pauseByError() {
        getLock().forceUnlock();
    }

    @Override
    public String getCurrentStatus() {
        return getBucket().get();
    }

    private RBucket<String> getBucket() {
        return redissonClient.getBucket(getExecutorKey());
    }

    private RLock getLock() {
        return redissonClient.getLock(getProcessingKey());
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `mvn test -Dtest=RedisExecutorManagerTest`
Expected: BUILD SUCCESS, all 4 tests pass

- [ ] **Step 3: Commit**

```bash
git add src/main/java/io/github/kugin/reconciliation/executor/RedisExecutorManager.java
git commit -m "feat: add RedisExecutorManager with Redisson-based distributed state management"
```

---

### Task 4: Write RedisResourceLoader tests

**Files:**
- Create: `src/test/java/io/github/kugin/reconciliation/before/RedisResourceLoaderTest.java`

- [ ] **Step 1: Create test class with embedded Redis setup**

```java
package io.github.kugin.reconciliation.before;

import io.github.kugin.reconciliation.domain.CheckAdapter;
import io.github.kugin.reconciliation.domain.CheckEntry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import redis.embedded.RedisServer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RedisResourceLoaderTest {

    private RedisServer redisServer;
    private RedissonClient redissonClient;

    @Before
    public void setUp() {
        redisServer = RedisServer.builder()
                .port(63791)
                .setting("maxmemory 128M")
                .build();
        redisServer.start();
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:63791");
        redissonClient = Redisson.create(config);
    }

    @After
    public void tearDown() {
        redissonClient.shutdown();
        redisServer.stop();
    }

    /** Simple entity for testing explicit field mapping */
    public static class TestItem {
        public String id;
        public String value;
        public TestItem(String id, String value) {
            this.id = id;
            this.value = value;
        }
    }

    @Test
    public void testKeyPatternFormat() {
        RedisResourceLoader<TestItem> loader = new RedisResourceLoader<>(
                "recon:src:%s", "id", Arrays.asList("value"),
                line -> {
                    String[] parts = line.split(",");
                    return new TestItem(parts[0], parts[1]);
                },
                redissonClient
        );
        Assert.assertEquals("recon:src:20260426", loader.getRedisKey("20260426"));
    }

    @Test
    public void testLoadWithExplicitFields() {
        RList<String> list = redissonClient.getList("recon:src:20260426");
        list.addAll(Arrays.asList("a1,val1", "a2,val2", "a3,val3"));

        RedisResourceLoader<TestItem> loader = new RedisResourceLoader<>(
                "recon:src:%s", "id", Arrays.asList("value"),
                line -> {
                    String[] parts = line.split(",");
                    return new TestItem(parts[0], parts[1]);
                },
                redissonClient
        );

        List<CheckEntry> entries = loader.load("20260426");
        Assert.assertEquals(3, entries.size());
        Assert.assertEquals("a1", entries.get(0).getKey());
        Map<String, Object> checkData = entries.get(0).getCheckData();
        Assert.assertEquals("val1", checkData.get("value"));
    }

    @Test
    public void testLoadAutoDetectWithAdapter() {
        RList<String> list = redissonClient.getList("recon:adapter:20260426");
        list.addAll(Arrays.asList("x1,data1", "x2,data2"));

        RedisResourceLoader<CheckAdapter> loader = new RedisResourceLoader<>(
                "recon:adapter:%s", null, null,
                line -> {
                    String[] parts = line.split(",");
                    String id = parts[0];
                    String data = parts[1];
                    return new CheckAdapter() {
                        @Override
                        public String getKey() { return id; }
                        @Override
                        public Map<String, Object> getCheckData() {
                            return java.util.Collections.singletonMap("data", data);
                        }
                    };
                },
                redissonClient
        );

        List<CheckEntry> entries = loader.load("20260426");
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals("x1", entries.get(0).getKey());
    }

    @Test
    public void testLoadEmptyList() {
        RedisResourceLoader<TestItem> loader = new RedisResourceLoader<>(
                "recon:empty:%s", "id", Arrays.asList("value"),
                line -> {
                    String[] parts = line.split(",");
                    return new TestItem(parts[0], parts[1]);
                },
                redissonClient
        );

        List<CheckEntry> entries = loader.load("20260426");
        Assert.assertTrue(entries.isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails (class doesn't exist yet)**

Run: `mvn test -Dtest=RedisResourceLoaderTest#testKeyPatternFormat`
Expected: COMPILATION ERROR — `RedisResourceLoader` does not exist

- [ ] **Step 3: Commit**

```bash
git add src/test/java/io/github/kugin/reconciliation/before/RedisResourceLoaderTest.java
git commit -m "test: add RedisResourceLoader tests (red, no impl yet)"
```

---

### Task 5: Implement RedisResourceLoader

**Files:**
- Create: `src/main/java/io/github/kugin/reconciliation/before/RedisResourceLoader.java`

- [ ] **Step 1: Write RedisResourceLoader**

```java
package io.github.kugin.reconciliation.before;

import cn.hutool.core.text.CharSequenceUtil;
import io.github.kugin.reconciliation.domain.CheckEntry;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RedisResourceLoader<T> implements ResourceLoader {

    private final String redisKeyPattern;
    private final String identityField;
    private final List<String> checkFields;
    private final RedisEntityParser<T> redisEntityParser;
    private final RedissonClient redissonClient;

    public RedisResourceLoader(String redisKeyPattern, String identityField,
                               List<String> checkFields, RedisEntityParser<T> redisEntityParser,
                               RedissonClient redissonClient) {
        this.redisKeyPattern = redisKeyPattern;
        this.identityField = identityField;
        this.checkFields = checkFields;
        this.redisEntityParser = redisEntityParser;
        this.redissonClient = redissonClient;
    }

    public String getRedisKey(String date) {
        return String.format(redisKeyPattern, date);
    }

    @Override
    public List<CheckEntry> load(String date) {
        RList<String> list = redissonClient.getList(getRedisKey(date));
        List<T> entities = list.readAll().stream()
                .map(redisEntityParser::parse)
                .collect(Collectors.toList());
        if (CharSequenceUtil.isEmpty(identityField) && (checkFields == null || checkFields.isEmpty())) {
            return CheckEntry.wrap(entities);
        }
        return CheckEntry.wrap(entities, identityField, checkFields);
    }

    @FunctionalInterface
    public interface RedisEntityParser<T> {
        T parse(String line);
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `mvn test -Dtest=RedisResourceLoaderTest`
Expected: BUILD SUCCESS, all 4 tests pass

- [ ] **Step 3: Commit**

```bash
git add src/main/java/io/github/kugin/reconciliation/before/RedisResourceLoader.java
git commit -m "feat: add RedisResourceLoader for loading reconciliation data from Redis"
```

---

### Task 6: Integration test with CheckExecutor and RedisExecutorManager

**Files:**
- Create: `src/test/java/io/github/kugin/reconciliation/executor/RedisCheckExecutorIntegrationTest.java`

- [ ] **Step 1: Create integration test using CheckExecutor with RedisExecutorManager**

```java
package io.github.kugin.reconciliation.executor;

import io.github.kugin.reconciliation.domain.CheckConfig;
import io.github.kugin.reconciliation.domain.CheckEntry;
import io.github.kugin.reconciliation.entry.TestA;
import io.github.kugin.reconciliation.entry.TestB;
import io.github.kugin.reconciliation.enums.ExecutorStatusEnum;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import redis.embedded.RedisServer;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class RedisCheckExecutorIntegrationTest {

    private RedisServer redisServer;
    private RedissonClient redissonClient;

    @Before
    public void setUp() {
        redisServer = RedisServer.builder()
                .port(63792)
                .setting("maxmemory 128M")
                .build();
        redisServer.start();
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:63792");
        redissonClient = Redisson.create(config);
    }

    @After
    public void tearDown() {
        redissonClient.shutdown();
        redisServer.stop();
    }

    @Test
    public void testExecutorWithRedisManager() {
        TestA a1 = new TestA("a1", "remark", 1d);
        TestA a2 = new TestA("a2", "remark", 1d);
        TestA a3 = new TestA("a3", "remark", 1d);

        TestB b1 = new TestB("a1", "remark", 1d);
        TestB b2 = new TestB("a2", "remark", 1d);
        TestB b3 = new TestB("b3", "remark", 1d);

        CheckConfig checkConfig = CheckConfig.builder()
                .srcLoader(date -> CheckEntry.wrap(Arrays.asList(a1, a2, a3)))
                .targetLoader(date -> CheckEntry.wrap(Arrays.asList(b1, b2, b3)))
                .checkPre(context -> true)
                .id("redis-integration")
                .checkAfter(context -> System.out.println(context.getCheckResult().getDiffDetails()))
                .build();

        ExecutorManager redisManager = new RedisExecutorManager("redis-integration", redissonClient);
        CheckExecutor executor = CheckExecutor.builder()
                .id("redis-integration")
                .beforeCheckProcessor(
                        new io.github.kugin.reconciliation.before.DefaultBeforeCheckProcessor(
                                new io.github.kugin.reconciliation.before.DefaultResourceReader(
                                        checkConfig.getSrcLoader(), checkConfig.getTargetLoader()),
                                checkConfig.getCheckPre()))
                .checkProcessor(new io.github.kugin.reconciliation.check.DefaultCheckProcessor())
                .afterCheckProcessor(
                        new io.github.kugin.reconciliation.after.DefaultAfterCheckProcessor(
                                checkConfig.getCheckSync(), checkConfig.getCheckAfter()))
                .checkConfig(checkConfig)
                .executorManager(redisManager)
                .build();

        executor.process("20260426");

        Assert.assertEquals(ExecutorStatusEnum.END.toString(), redisManager.getCurrentStatus());
        Assert.assertFalse(redisManager.isProcessing());
    }

    @Test
    public void testReentrantWithRedisManager() throws Exception {
        CheckConfig config = CheckConfig.builder()
                .srcLoader(date -> CheckEntry.wrap(Arrays.asList(new TestA("x", "y", 1d))))
                .targetLoader(date -> CheckEntry.wrap(Arrays.asList(new TestB("x", "y", 1d))))
                .checkPre(context -> true)
                .id("redis-reentrant")
                .checkAfter(context -> {
                    try { Thread.sleep(3000); } catch (InterruptedException e) { }
                })
                .build();

        RedisExecutorManager manager1 = new RedisExecutorManager("redis-reentrant", redissonClient);
        CheckExecutor executor1 = CheckExecutor.builder()
                .id("redis-reentrant")
                .beforeCheckProcessor(
                        new io.github.kugin.reconciliation.before.DefaultBeforeCheckProcessor(
                                new io.github.kugin.reconciliation.before.DefaultResourceReader(
                                        config.getSrcLoader(), config.getTargetLoader()),
                                config.getCheckPre()))
                .checkProcessor(new io.github.kugin.reconciliation.check.DefaultCheckProcessor())
                .afterCheckProcessor(
                        new io.github.kugin.reconciliation.after.DefaultAfterCheckProcessor(
                                config.getCheckSync(), config.getCheckAfter()))
                .checkConfig(config)
                .executorManager(manager1)
                .build();

        // Start first executor, wait briefly, then try a second
        CompletableFuture<Void> f1 = CompletableFuture.runAsync(() -> executor1.process("20260426"));
        Thread.sleep(500);

        RedisExecutorManager manager2 = new RedisExecutorManager("redis-reentrant", redissonClient);
        CheckExecutor executor2 = CheckExecutor.builder()
                .id("redis-reentrant")
                .beforeCheckProcessor(
                        new io.github.kugin.reconciliation.before.DefaultBeforeCheckProcessor(
                                new io.github.kugin.reconciliation.before.DefaultResourceReader(
                                        config.getSrcLoader(), config.getTargetLoader()),
                                config.getCheckPre()))
                .checkProcessor(new io.github.kugin.reconciliation.check.DefaultCheckProcessor())
                .afterCheckProcessor(
                        new io.github.kugin.reconciliation.after.DefaultAfterCheckProcessor(
                                config.getCheckSync(), config.getCheckAfter()))
                .checkConfig(config)
                .executorManager(manager2)
                .build();

        CompletableFuture<Void> f2 = CompletableFuture.runAsync(() -> executor2.process("20260426"));
        f1.join();
        f2.join();

        // First executor completed successfully
        Assert.assertEquals(ExecutorStatusEnum.END.toString(), manager1.getCurrentStatus());
        Assert.assertFalse(manager1.isProcessing());
    }
}
```

- [ ] **Step 2: Run integration tests**

Run: `mvn test -Dtest=RedisCheckExecutorIntegrationTest`
Expected: BUILD SUCCESS, both tests pass

- [ ] **Step 3: Run all tests to confirm no regressions**

Run: `mvn test`
Expected: BUILD SUCCESS, all 12 tests pass (4 existing + 4 RedisExecutorManager + 4 RedisResourceLoader + 2 integration — actually 8 existing + 12 new = 10 total)

- [ ] **Step 4: Commit**

```bash
git add src/test/java/io/github/kugin/reconciliation/executor/RedisCheckExecutorIntegrationTest.java
git commit -m "test: add Redis CheckExecutor integration tests"
```

- [ ] **Step 5: Update README to mark TODOs complete**

Replace the TODO lines:
```
### TODO:
- [ ] 基于redis的对账处理器实现
- [ ] 基于redis的执行器管理
```

with:
```
### TODO:
- [x] 基于redis的对账处理器实现
- [x] 基于redis的执行器管理
```

```bash
git add README.md
git commit -m "docs: mark Redis TODOs as completed"
```
