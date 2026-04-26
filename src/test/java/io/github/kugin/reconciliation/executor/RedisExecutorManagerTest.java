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
