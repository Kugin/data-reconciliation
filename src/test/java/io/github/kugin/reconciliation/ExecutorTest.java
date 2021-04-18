package io.github.kugin.reconciliation;

import cn.hutool.cache.impl.FIFOCache;
import io.github.kugin.reconciliation.domain.CheckConfig;
import io.github.kugin.reconciliation.domain.CheckEntry;
import io.github.kugin.reconciliation.entry.TestA;
import io.github.kugin.reconciliation.entry.TestB;
import io.github.kugin.reconciliation.enums.ExecutorStatusEnum;
import io.github.kugin.reconciliation.executor.CheckExecutor;
import io.github.kugin.reconciliation.executor.SimpleCache;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * @author Kugin
 */
public class ExecutorTest {

    private CheckConfig checkConfig;

    @Before
    public void before() {
        TestA a1 = new TestA("a1", "remark", 1d);
        TestA a2 = new TestA("a2", "remark", 1d);
        TestA a3 = new TestA("a3", "remark", 1d);

        TestB b1 = new TestB("a1", "remark", 1d);
        TestB b2 = new TestB("a2", "remark", 1d);
        TestB b3 = new TestB("b3", "remark", 1d);
        checkConfig = CheckConfig.builder()
                .srcLoader(date -> CheckEntry.wrap(Arrays.asList(a1, a2, a3)))
                .targetLoader(date -> CheckEntry.wrap(Arrays.asList(b1, b2, b3)))
                .checkPre(context -> true)
                .id("test")
                //.checkSync(null)
                .checkAfter(context -> System.out.println(context.getCheckResult().getDiffDetails()))
                .build();
    }

    @Test
    public void testExecutor() {
        CheckExecutor executor = CheckExecutor.buildExecutor(checkConfig);
        executor.process("20210204");
        Assert.assertNotNull(executor.getExecutorManager().getCurrentStatus());
        Assert.assertEquals(ExecutorStatusEnum.END.toString(), executor.getExecutorManager().getCurrentStatus());
    }

    @Test
    public void testSimpleCache() {
        FIFOCache<String, Object> instance = SimpleCache.getInstance();
        FIFOCache<String, Object> instance1 = SimpleCache.getInstance();
        Assert.assertEquals(instance, instance1);
    }

    @Test
    public void testReentrant() {
        checkConfig.setId("AAA");
        checkConfig.setCheckAfter(context -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        CheckExecutor aaa = CheckExecutor.buildExecutor(checkConfig);
        CheckExecutor aaaCopy = CheckExecutor.buildExecutor(checkConfig);
        //aaa.process("20210204");
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> aaa.process("20210204"));
        //线程睡眠模拟上一个任务开始但没执行完的情况
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //重复执行同一个id的executors,阻断
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> aaaCopy.process("20210204"));

        future.join();
        future1.join();
    }
}
