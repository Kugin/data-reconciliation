package io.github.kugin.reconciliation.after;

import io.github.kugin.reconciliation.domain.CheckContext;
import io.github.kugin.reconciliation.domain.CheckUnit;
import io.github.kugin.reconciliation.enums.CheckSyncEnum;

import java.util.Objects;

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
        if (Objects.nonNull(checkAfter)) {
            checkAfter.doAfter(context);
        }
    }

    @Override
    public CheckSyncEnum syncSourceMore(CheckUnit checkUnit) {
        if (Objects.isNull(checkSync)) {
            return CheckSyncEnum.SYNC;
        }
        return checkSync.syncSourceMore(checkUnit);
    }

    @Override
    public CheckSyncEnum syncTargetMore(CheckUnit checkUnit) {
        if (Objects.isNull(checkSync)) {
            return CheckSyncEnum.SYNC;
        }
        return checkSync.syncTargetMore(checkUnit);
    }

    @Override
    public CheckSyncEnum syncDifferent(CheckUnit checkUnit) {
        if (Objects.isNull(checkSync)) {
            return CheckSyncEnum.SYNC;
        }
        return checkSync.syncDifferent(checkUnit);
    }

    @Override
    public boolean isComplete(CheckContext context) {
        return Objects.isNull(checkSync) || checkSync.isComplete(context);
    }
}
