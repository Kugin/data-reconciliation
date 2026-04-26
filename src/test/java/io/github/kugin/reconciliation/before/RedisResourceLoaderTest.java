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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RedisResourceLoaderTest {

    private RedisServer redisServer;
    private RedissonClient redissonClient;

    @Before
    public void setUp() throws IOException {
        redisServer = RedisServer.newRedisServer()
                .port(63791)
                .setting("maxmemory 128M")
                .build();
        redisServer.start();
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:63791");
        redissonClient = Redisson.create(config);
    }

    @After
    public void tearDown() throws IOException {
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
