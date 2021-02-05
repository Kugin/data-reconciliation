package io.github.kugin.reconciliation.check;

import io.github.kugin.reconciliation.domain.CheckContext;

/**
 * 对账处理器
 *
 * @author Kugin
 */
public interface CheckProcessor {

    /**
     * 对账逻辑
     *
     * @param context 上下文
     */
    void compare(CheckContext context);
}
