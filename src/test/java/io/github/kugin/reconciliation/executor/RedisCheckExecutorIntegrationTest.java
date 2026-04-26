package io.github.kugin.reconciliation.executor;

import io.github.kugin.reconciliation.after.DefaultAfterCheckProcessor;
import io.github.kugin.reconciliation.before.DefaultBeforeCheckProcessor;
import io.github.kugin.reconciliation.before.DefaultResourceReader;
import io.github.kugin.reconciliation.check.DefaultCheckProcessor;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class RedisCheckExecutorIntegrationTest {

    private RedisServer redisServer;
    private RedissonClient redissonClient;

    @Before
    public void setUp() throws IOException {
        redisServer = RedisServer.newRedisServer()
                .port(63792)
                .setting("maxmemory 128M")
                .build();
        redisServer.start();
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:63792");
        redissonClient = Redisson.create(config);
    }

    @After
    public void tearDown() throws IOException {
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
                        new DefaultBeforeCheckProcessor(
                                new DefaultResourceReader(
                                        checkConfig.getSrcLoader(), checkConfig.getTargetLoader()),
                                checkConfig.getCheckPre()))
                .checkProcessor(new DefaultCheckProcessor())
                .afterCheckProcessor(
                        new DefaultAfterCheckProcessor(
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
                        new DefaultBeforeCheckProcessor(
                                new DefaultResourceReader(
                                        config.getSrcLoader(), config.getTargetLoader()),
                                config.getCheckPre()))
                .checkProcessor(new DefaultCheckProcessor())
                .afterCheckProcessor(
                        new DefaultAfterCheckProcessor(
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
                        new DefaultBeforeCheckProcessor(
                                new DefaultResourceReader(
                                        config.getSrcLoader(), config.getTargetLoader()),
                                config.getCheckPre()))
                .checkProcessor(new DefaultCheckProcessor())
                .afterCheckProcessor(
                        new DefaultAfterCheckProcessor(
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
