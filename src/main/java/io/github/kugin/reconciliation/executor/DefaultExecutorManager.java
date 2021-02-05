package io.github.kugin.reconciliation.executor;

import cn.hutool.cache.impl.FIFOCache;
import io.github.kugin.reconciliation.enums.ExecutorStatusEnum;

/**
 * @author Kugin
 */
public class DefaultExecutorManager extends AbstractExecutorManager {

    private FIFOCache<String, Object> cache = SimpleCache.getInstance();

    public DefaultExecutorManager(String id) {
        super(id);
    }

    @Override
    public boolean isProcessing() {
        return cache.containsKey(getProcessingKey());
    }

    @Override
    public boolean isComplete() {
        String key = getExecutorKey();
        if (!cache.containsKey(key)) {
            return false;
        }
        String status = getCurrentStatus();
        return status.equals(ExecutorStatusEnum.END.toString());
    }

    @Override
    public void setStatus(ExecutorStatusEnum status) {
        if (status.equals(ExecutorStatusEnum.BEFORE)) {
            cache.put(getProcessingKey(), true);
        }
        if (status.equals(ExecutorStatusEnum.END)) {
            cache.remove(getProcessingKey());
        }
        cache.put(getExecutorKey(), status.toString());
    }

    @Override
    public void pauseByError() {
        cache.remove(getProcessingKey());
    }

    @Override
    public String getCurrentStatus() {
        return (String) cache.get(getExecutorKey());
    }
}
