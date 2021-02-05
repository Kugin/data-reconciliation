package io.github.kugin.reconciliation.executor;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.FIFOCache;

/**
 * 简单缓存-单例
 *
 * @author Kugin
 * @see DefaultExecutorManager 使用
 */
public class SimpleCache {

    private SimpleCache() {
        //do-nothing
    }

    public static FIFOCache<String, Object> getInstance() {
        return SimpleCacheHolder.instance;
    }

    private static class SimpleCacheHolder {
        private static final FIFOCache<String, Object> instance = CacheUtil.newFIFOCache(100);
    }
}
