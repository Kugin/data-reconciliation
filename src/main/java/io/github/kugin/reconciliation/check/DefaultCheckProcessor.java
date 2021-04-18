package io.github.kugin.reconciliation.check;

import io.github.kugin.reconciliation.domain.CheckContext;
import io.github.kugin.reconciliation.domain.CheckEntry;
import io.github.kugin.reconciliation.domain.CheckResult;
import io.github.kugin.reconciliation.domain.CheckUnit;
import io.github.kugin.reconciliation.enums.CheckStateEnum;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Kugin
 */
public class DefaultCheckProcessor extends AbstractCheckProcessor {

    @Override
    public void doCompare(CheckContext context) {
        Map<String, CheckEntry> sourceMap = context.getSourceMap();
        Map<String, CheckEntry> targetMap = context.getTargetMap();
        List<CheckUnit> unitDetails = compareMap(sourceMap, targetMap);
        Map<CheckStateEnum, List<CheckUnit>> unitdetailsMap = unitDetails.stream().collect(Collectors.groupingBy(CheckUnit::getState));
        Map<String, CheckEntry> sourceDiffMap = Optional.ofNullable(unitdetailsMap.get(CheckStateEnum.SOURCE_MORE)).orElse(Collections.emptyList())
                .stream().collect(Collectors.toMap(CheckUnit::getKey, unit -> sourceMap.get(unit.getKey())));
        Map<String, CheckEntry> targetDiffMap = Optional.ofNullable(unitdetailsMap.get(CheckStateEnum.TARGET_MORE)).orElse(Collections.emptyList()).stream()
                .collect(Collectors.toMap(CheckUnit::getKey, unit -> targetMap.get(unit.getKey())));
        CheckResult result = CheckResult.builder()
                .sourceDiffMap(sourceDiffMap)
                .targetDiffMap(targetDiffMap)
                .diffDetails(unitDetails)
                .diffDetailsMap(unitdetailsMap)
                .build();
        context.setCheckResult(result);
    }
}
