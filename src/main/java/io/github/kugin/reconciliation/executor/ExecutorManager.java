package io.github.kugin.reconciliation.executor;

import io.github.kugin.reconciliation.enums.ExecutorStatusEnum;

/**
 * 对账生命周期管理
 *
 * @author Kugin
 */
public interface ExecutorManager {

    /**
     * 初始化执行日期
     */
    void initDate(String date);

    /**
     * 执行器标识
     */
    String getExecutorKey();

    /**
     * 执行器执行标识
     */
    String getProcessingKey();

    /**
     * 执行器是否正在执行
     */
    boolean isProcessing();

    /**
     * 执行器是否结束
     */
    boolean isComplete();

    /**
     * 设置执行器状态
     *
     * @param status 状态
     */
    void setStatus(ExecutorStatusEnum status);

    /**
     * 执行器异常失败
     */
    void pauseByError();

    /**
     * 获取当前执行器状态
     */
    String getCurrentStatus();
}
