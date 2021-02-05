package io.github.kugin.reconciliation.before;

import io.github.kugin.reconciliation.domain.CheckContext;
import io.github.kugin.reconciliation.domain.CheckEntry;
import io.github.kugin.reconciliation.enums.ExecutorStatusEnum;
import io.github.kugin.reconciliation.executor.ExecutorManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Kugin
 */
public abstract class AbstractBeforeCheckProcessor implements BeforeCheckProcessor, CheckPre, ResourceReader {

    public List<CheckEntry> getSource(String date) {
        return getSourceLoader().load(date);
    }

    public List<CheckEntry> getTarget(String date) {
        return getTargetLoader().load(date);
    }

    public List<CheckEntry> getDifferent(String date) {
        ResourceLoader differentLoader = getDifferentLoader();
        if (differentLoader != null) {
            return differentLoader.load(date);
        }
        return Collections.emptyList();
    }


    @Override
    public void processBeforeCompare(CheckContext context) {
        ExecutorManager executorManager = context.getExecutorManager();
        executorManager.setStatus(ExecutorStatusEnum.BEFORE);
        String date = context.getDate();
        //前置校验不通过
        if (!check(date)) {
            return;
        }
        List<CheckEntry> different = getDifferent(date);
        if (different != null) {
            //todo 差异数据处理,重复对比
        }
        //todo forkjoinPool 调整 (实际源函数可能已经有子线程池,此时数据拉取的性能待测试)
        CompletableFuture<List<CheckEntry>> sourceFuture = CompletableFuture.supplyAsync(() -> getSource(date));
        CompletableFuture<List<CheckEntry>> targetFuture = CompletableFuture.supplyAsync(() -> getTarget(date));
        List<CheckEntry> source = sourceFuture.join();
        List<CheckEntry> target = targetFuture.join();
        //todo 按key去重
        Map<String, CheckEntry> sourceMap = source.stream().collect(Collectors.toMap(CheckEntry::getKey, Function.identity()));
        Map<String, CheckEntry> targetMap = target.stream().collect(Collectors.toMap(CheckEntry::getKey, Function.identity()));
        context.setSource(source);
        context.setTarget(target);
        context.setSourceMap(sourceMap);
        context.setTargetMap(targetMap);
    }
}
