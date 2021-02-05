package io.github.kugin.reconciliation.after;

import io.github.kugin.reconciliation.domain.CheckContext;
import io.github.kugin.reconciliation.domain.CheckResult;
import io.github.kugin.reconciliation.domain.CheckUnit;

/**
 * @author Kugin
 */
public class DefaultAfterCheckProcessor extends AbstractAfterCheckProcessor {
    private CheckSync checkSync;

    private CheckAfter checkAfter;

    public DefaultAfterCheckProcessor(CheckSync checkSync, CheckAfter checkAfter) {
        this.checkSync = checkSync;
        this.checkAfter = checkAfter;
    }

    @Override
    public void doAfter(CheckContext context) {
        if (checkAfter != null) {
            checkAfter.doAfter(context);
        }
    }

    @Override
    public void syncSourceMore(CheckUnit checkUnit) {
        if (checkSync != null) {
            checkSync.syncSourceMore(checkUnit);
        }
    }

    @Override
    public void syncTargetMore(CheckUnit checkUnit) {
        if (checkSync != null) {
            checkSync.syncTargetMore(checkUnit);
        }
    }

    @Override
    public void syncDifferent(CheckUnit checkUnit) {
        if (checkSync != null) {
            checkSync.syncDifferent(checkUnit);
        }
    }

    @Override
    public boolean isComplete(CheckResult checkResult) {
        if (checkSync != null) {
            return checkSync.isComplete(checkResult);
        }
        return true;
    }
}
