package io.github.kugin.reconciliation.after;

import io.github.kugin.reconciliation.domain.CheckContext;
import io.github.kugin.reconciliation.domain.CheckResult;
import io.github.kugin.reconciliation.domain.CheckUnit;
import io.github.kugin.reconciliation.enums.CheckStateEnum;
import io.github.kugin.reconciliation.enums.CheckSyncEnum;
import io.github.kugin.reconciliation.enums.ExecutorStatusEnum;
import io.github.kugin.reconciliation.executor.ExecutorManager;

import java.util.List;

/**
 * @author Kugin
 */
public abstract class AbstractAfterCheckProcessor implements AfterCheckProcessor, CheckSync, CheckAfter {
    @Override
    public void processAfterCompare(CheckContext context) {
        ExecutorManager executorManager = context.getExecutorManager();
        executorManager.setStatus(ExecutorStatusEnum.AFTER);
        CheckResult checkResult = context.getCheckResult();
        autoSync(checkResult);
        if (isComplete(context)) {
            executorManager.setStatus(ExecutorStatusEnum.END);
        } else {
            executorManager.setStatus(ExecutorStatusEnum.ERROR);
        }
        doAfter(context);
    }

    /**
     * 自动调账
     *
     * @param checkResult 对账结果
     */
    public void autoSync(CheckResult checkResult) {
        List<CheckUnit> diffDetails = checkResult.getDiffDetails();
        diffDetails.forEach(checkUnit -> {
            if (CheckStateEnum.DIFFERENT.equals(checkUnit.getState())) {
                checkUnit.setSync(syncDifferent(checkUnit));
            }
            if (CheckStateEnum.SOURCE_MORE.equals(checkUnit.getState())) {
                checkUnit.setSync(syncSourceMore(checkUnit));
            }
            if (CheckStateEnum.TARGET_MORE.equals(checkUnit.getState())) {
                checkUnit.setSync(syncTargetMore(checkUnit));
            }
        });
    }
}
