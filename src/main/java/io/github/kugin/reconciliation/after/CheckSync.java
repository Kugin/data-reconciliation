package io.github.kugin.reconciliation.after;

import io.github.kugin.reconciliation.domain.CheckResult;
import io.github.kugin.reconciliation.domain.CheckUnit;
import io.github.kugin.reconciliation.enums.CheckSyncEnum;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 差错处理
 *
 * @author Kugin
 */
public interface CheckSync {

    /**
     * 同步处理源数据多的情况
     *
     * @param checkUnit 对账单元
     */
    void syncSourceMore(CheckUnit checkUnit);

    /**
     * 同步处理目标数据多的情况
     *
     * @param checkUnit 对账单元
     */
    void syncTargetMore(CheckUnit checkUnit);

    /**
     * 同步处理源数据与目标数据有差异的情况
     *
     * @param checkUnit 对账单元
     */
    void syncDifferent(CheckUnit checkUnit);

    /**
     * 对账是否通过
     *
     * @param checkResult 对账结果
     * @return 对账是否通过
     */
    default boolean isComplete(CheckResult checkResult) {
        List<CheckUnit> unSyncList = checkResult.getDiffDetails().stream().filter(v -> CheckSyncEnum.NO_SYNC.equals(v.getSync())).collect(Collectors.toList());
        return unSyncList.isEmpty();
    }
}
