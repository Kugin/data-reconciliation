package io.github.kugin.reconciliation.after;

import io.github.kugin.reconciliation.domain.CheckContext;

/**
 * 对账后置处理器
 *
 * @author Kugin
 */
public interface AfterCheckProcessor {

    /**
     * 对账结束后需要处理的逻辑
     *
     * @param context 上下文
     */
    void processAfterCompare(CheckContext context);

}
