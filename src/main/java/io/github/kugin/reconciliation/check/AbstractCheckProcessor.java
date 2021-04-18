package io.github.kugin.reconciliation.check;


import io.github.kugin.reconciliation.domain.*;
import io.github.kugin.reconciliation.enums.CheckStateEnum;
import io.github.kugin.reconciliation.enums.CheckSyncEnum;
import io.github.kugin.reconciliation.enums.ExecutorStatusEnum;
import io.github.kugin.reconciliation.executor.ExecutorManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kugin
 */
public abstract class AbstractCheckProcessor implements CheckProcessor {

    public void compareCheck(CheckContext context) {
        List<CheckEntry> source = context.getSource();
        List<CheckEntry> target = context.getTarget();
        //都为空时,继续走
        if (source.isEmpty() && target.isEmpty()){
            return;
        }
        //单个为空时,认为数据没准备好
        if (source.isEmpty() || target.isEmpty()) {
            throw new CheckException("对账数据未准备好");
        }
    }

    public abstract void doCompare(CheckContext context);

    @Override
    public void compare(CheckContext context) {
        ExecutorManager executorManager = context.getExecutorManager();
        executorManager.setStatus(ExecutorStatusEnum.CHECK);

        compareCheck(context);
        doCompare(context);
    }

    /**
     * 比较两个Map数据的不同
     *
     * @param sourceMap 源数据
     * @param targetMap 目标数据
     * @return
     */
    protected List<CheckUnit> compareMap(Map<String, CheckEntry> sourceMap, Map<String, CheckEntry> targetMap) {
        List<CheckUnit> unitDetails = new ArrayList<>();
        sourceMap.forEach((k, v) -> {
            if (!targetMap.containsKey(k)) {
                unitDetails.add(CheckUnit.builder()
                        .key(k)
                        .state(CheckStateEnum.SOURCE_MORE)
                        .sync(CheckSyncEnum.NO_SYNC)
                        .differentFields(createSingle(v.getCheckData(), true))
                        .build());
                return;
            }
            Map<String, FieldkUnit> fieldkUnitMap = compareCheckData(v.getCheckData(), targetMap.get(k).getCheckData());
            if (fieldkUnitMap == null || fieldkUnitMap.isEmpty()) {
                return;
            }
            unitDetails.add(CheckUnit.builder()
                    .key(k)
                    .state(CheckStateEnum.DIFFERENT)
                    .sync(CheckSyncEnum.NO_SYNC)
                    .differentFields(fieldkUnitMap)
                    .build());
        });
        targetMap.forEach((k, v) -> {
            if (!sourceMap.containsKey(k)) {
                unitDetails.add(CheckUnit.builder()
                        .key(k)
                        .state(CheckStateEnum.TARGET_MORE)
                        .sync(CheckSyncEnum.NO_SYNC)
                        .differentFields(createSingle(v.getCheckData(), false))
                        .build());
            }

        });
        return unitDetails;
    }

    /**
     * 比较两个 checkData 的详细属性不同
     *
     * @param sourceData 来源 checkData
     * @param targetData 目标 checkData
     * @return
     */
    public Map<String, FieldkUnit> compareCheckData(Map<String, Object> sourceData, Map<String, Object> targetData) {
        Map<String, FieldkUnit> differnt = new HashMap<>();
        sourceData.forEach((k, v) -> {
            if (targetData.containsKey(k) && v.equals(targetData.get(k))) {
                return;
            }
            differnt.put(k, FieldkUnit.builder().fieldName(k).source(v).target(targetData.get(k)).build());
        });
        targetData.forEach((k, v) -> {
            if (sourceData.containsKey(k)) {
                return;
            }
            differnt.put(k, FieldkUnit.builder().fieldName(k).target(v).build());
        });
        return differnt;
    }

    /**
     * source_more 与 target_more 的情况下,将checkData映射为fieldkUnitMap 放入checkUnit
     * 方便使用拿到原始值, 方判断是否需要处理
     *
     * @param checkData 原始值
     * @param isSource  数据来源: true:源, false:目标
     * @return
     */
    public Map<String, FieldkUnit> createSingle(Map<String, Object> checkData, boolean isSource) {
        Map<String, FieldkUnit> differnt = new HashMap<>();
        checkData.forEach((k, v) -> {
            if (isSource) {
                differnt.put(k, FieldkUnit.builder().fieldName(k).source(v).build());
                return;
            }
            differnt.put(k, FieldkUnit.builder().fieldName(k).target(v).build());
        });
        return differnt;
    }
}
