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
