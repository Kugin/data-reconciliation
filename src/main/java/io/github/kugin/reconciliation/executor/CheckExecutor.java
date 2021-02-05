package io.github.kugin.reconciliation.executor;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.map.MapUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import io.github.kugin.reconciliation.after.AfterCheckProcessor;
import io.github.kugin.reconciliation.after.DefaultAfterCheckProcessor;
import io.github.kugin.reconciliation.before.BeforeCheckProcessor;
import io.github.kugin.reconciliation.before.DefaultBeforeCheckProcessor;
import io.github.kugin.reconciliation.before.DefaultResourceReader;
import io.github.kugin.reconciliation.before.ResourceReader;
import io.github.kugin.reconciliation.check.CheckProcessor;
import io.github.kugin.reconciliation.check.DefaultCheckProcessor;
import io.github.kugin.reconciliation.domain.CheckConfig;
import io.github.kugin.reconciliation.domain.CheckContext;
import io.github.kugin.reconciliation.domain.CheckResult;
import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

/**
 * 对账执行器
 *
 * @author Kugin
 */
@Builder
@Getter
public class CheckExecutor {

    private static final Log log = LogFactory.get();

    private final String id;

    private final BeforeCheckProcessor beforeCheckProcessor;

    private final CheckProcessor checkProcessor;

    private final AfterCheckProcessor afterCheckProcessor;

    private final CheckConfig checkConfig;

    private final ExecutorManager executorManager;

    /**
     * 创建执行器
     *
     * @param checkConfig 配置参数
     * @return 执行器
     */
    public static CheckExecutor buildExecutor(CheckConfig checkConfig) {
        //前置处理器组装
        ResourceReader resourceReader = Optional.ofNullable(checkConfig.getResourceReader())
                .orElse(new DefaultResourceReader(checkConfig.getSrcLoader(), checkConfig.getTargetLoader()));
        BeforeCheckProcessor beforeCheckProcessor = new DefaultBeforeCheckProcessor(resourceReader, checkConfig.getCheckPre());

        //对账处理器组装
        CheckProcessor checkProcessor = Optional.ofNullable(checkConfig.getCheckProcessor()).orElse(new DefaultCheckProcessor());

        //后置处理器组装
        AfterCheckProcessor afterCheckProcessor = new DefaultAfterCheckProcessor(checkConfig.getCheckSync(), checkConfig.getCheckAfter());

        //对账管理器组装
        String id = Optional.ofNullable(checkConfig.getId()).orElse(checkConfig.getName());
        ExecutorManager executorManager = new DefaultExecutorManager(id);
        return CheckExecutor.builder()
                .id(id)
                .beforeCheckProcessor(beforeCheckProcessor)
                .checkProcessor(checkProcessor)
                .afterCheckProcessor(afterCheckProcessor)
                .checkConfig(checkConfig)
                .executorManager(executorManager)
                .build();
    }

    /**
     * 执行器业务逻辑
     */
    public void process(String date) {
        executorManager.initDate(date);
        CheckContext checkContext = CheckContext.builder()
                .name(checkConfig.getName())
                .date(date)
                .executorManager(executorManager)
                .build();
        log.info("对账开始:", executorManager.getExecutorKey());
        StopWatch stopWatch = new StopWatch(id);
        try {
            if (executorManager.isProcessing()) {
                //任务正在执行中
                log.info("当前任务正在执行,本次执行忽略", executorManager.getExecutorKey());
                return;
            }
            stopWatch.start(beforeCheckProcessor.getClass().getName());

            //todo 细节日志补充.
            beforeCheckProcessor.processBeforeCompare(checkContext);

            stopWatch.stop();
            stopWatch.start(checkProcessor.getClass().getName());

            checkProcessor.compare(checkContext);

            CheckResult result = checkContext.getCheckResult();
            log.info("对账结果:", executorManager.getExecutorKey(), MapUtil.of(Pair.of("sourceSize", checkContext.getSource().size()),
                    Pair.of("targetSize", checkContext.getTarget().size()),
                    Pair.of("different", result.getDiffDetails().size())));
            stopWatch.stop();
            stopWatch.start(afterCheckProcessor.getClass().getName());

            afterCheckProcessor.processAfterCompare(checkContext);
            stopWatch.stop();
        } catch (Exception e) {
            log.warn("对账异常结束", executorManager.getExecutorKey() + ":" + executorManager.getCurrentStatus(), e);
            executorManager.pauseByError();
        }
        log.info(stopWatch.prettyPrint());
        log.info("对账结束:", executorManager.getExecutorKey());
    }
}
