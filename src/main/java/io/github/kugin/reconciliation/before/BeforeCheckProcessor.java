package io.github.kugin.reconciliation.before;

import io.github.kugin.reconciliation.domain.CheckContext;

/**
 * 前置处理器
 * 准备对账的上下游数据
 *
 * @author Kugin
 */
public interface BeforeCheckProcessor {

    /**
     * 准备数据
     *
     * @param context
     * @return
     */
    void processBeforeCompare(CheckContext context);

}
