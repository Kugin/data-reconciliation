package io.github.kugin.reconciliation.before;

import io.github.kugin.reconciliation.domain.CheckContext;

/**
 * @author Kugin
 */
@FunctionalInterface
public interface CheckPre {
    /**
     * 前置处理逻辑
     *
     * @param context 上下文
     * @return
     */
    boolean doPre(CheckContext context);
}
