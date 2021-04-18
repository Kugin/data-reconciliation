package io.github.kugin.reconciliation.domain;

import cn.hutool.core.date.StopWatch;
import io.github.kugin.reconciliation.executor.ExecutorManager;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 对账上下文
 *
 * @author Kugin
 */
@Data
@Builder
public class CheckContext {

    /**
     * 对账id
     */
    private String id;

    /**
     * 对账名称
     */
    private String name;

    /**
     * 对账日期
     */
    private String date;

    /**
     * 对账源数据
     */
    private List<CheckEntry> source;

    /**
     * 对账目标数据
     */
    private List<CheckEntry> target;

    /**
     * 对账源数据
     */
    private Map<String, CheckEntry> sourceMap;

    /**
     * 对账目标数据
     */
    private Map<String, CheckEntry> targetMap;

    /**
     * 对账结果
     */
    private CheckResult checkResult;

    /**
     * 周期管理
     */
    private ExecutorManager executorManager;

    /**
     * 执行时间
     */
    private StopWatch stopWatch;
}
