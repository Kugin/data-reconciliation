package io.github.kugin.reconciliation.after;

import io.github.kugin.reconciliation.domain.CheckContext;

/**
 * @author Kugin
 */
@FunctionalInterface
public interface CheckAfter {
    /**
     * 后置处理逻辑
     *
     * @param context 上下文
     */
    void doAfter(CheckContext context);
}
