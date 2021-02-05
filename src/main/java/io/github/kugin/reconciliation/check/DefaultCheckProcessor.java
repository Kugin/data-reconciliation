package io.github.kugin.reconciliation.check;

import io.github.kugin.reconciliation.domain.CheckContext;
import io.github.kugin.reconciliation.domain.CheckEntry;
import io.github.kugin.reconciliation.domain.CheckResult;
import io.github.kugin.reconciliation.domain.CheckUnit;
import io.github.kugin.reconciliation.enums.CheckStateEnum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kugin
 */
public class DefaultCheckProcessor extends AbstractCheckProcessor{

    @Override
    public void doCompare(CheckContext context) {
        Map<String, CheckEntry> sourceMap = context.getSourceMap();
        Map<String, CheckEntry> targetMap = context.getTargetMap();
        List<CheckUnit> unitDetails = compareMap(sourceMap, targetMap);
        Map<String, CheckEntry> sourceDiffMap = new HashMap<>();
        Map<String, CheckEntry> targetDiffMap = new HashMap<>();
        unitDetails.forEach(unit -> {
            if (CheckStateEnum.SOURCE_MORE.equals(unit.getState())) {
                sourceDiffMap.put(unit.getKey(), sourceMap.get(unit.getKey()));
            }
            if (CheckStateEnum.TARGET_MORE.equals(unit.getState())) {
                targetDiffMap.put(unit.getKey(), targetMap.get(unit.getKey()));
            }
        });
        CheckResult result = CheckResult.builder()
                .sourceDiffMap(sourceDiffMap)
                .targetDiffMap(targetDiffMap)
                .diffDetails(unitDetails)
                .build();
        context.setCheckResult(result);
    }
}
