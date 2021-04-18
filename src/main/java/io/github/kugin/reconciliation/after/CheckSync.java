package io.github.kugin.reconciliation.after;

import io.github.kugin.reconciliation.domain.CheckContext;
import io.github.kugin.reconciliation.domain.CheckResult;
import io.github.kugin.reconciliation.domain.CheckUnit;
import io.github.kugin.reconciliation.enums.CheckSyncEnum;

import java.util.Objects;

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
    CheckSyncEnum syncSourceMore(CheckUnit checkUnit);

    /**
     * 同步处理目标数据多的情况
     *
     * @param checkUnit 对账单元
     */
    CheckSyncEnum syncTargetMore(CheckUnit checkUnit);

    /**
     * 同步处理源数据与目标数据有差异的情况
     *
     * @param checkUnit 对账单元
     */
    CheckSyncEnum syncDifferent(CheckUnit checkUnit);

    /**
     * 对账是否通过
     * <p>
     * 此处入参不是checkResult,是为了解决,对比过程中,没有不同,但实际状态又不能认为成功的情况:如支付中 两边都是pay_wait的情况,对比不出来,但又不能视为通过
     *
     * @param context 对账上下文
     * @return 对账是否通过
     */
    default boolean isComplete(CheckContext context) {
        CheckResult checkResult = context.getCheckResult();
        if (checkResult == null) {
            return false;
        }
        return checkResult.getDiffDetails().stream().noneMatch(v -> Objects.equals(CheckSyncEnum.NO_SYNC, v.getSync()));
    }
}
