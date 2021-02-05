package io.github.kugin.reconciliation.domain;

import io.github.kugin.reconciliation.enums.CheckStateEnum;
import io.github.kugin.reconciliation.enums.CheckSyncEnum;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

import java.util.Map;

/**
 * 每个实体的比较单元
 *
 * @author Kugin
 */
@Data
@Builder
public class CheckUnit {

    /**
     * 比较单元的唯一标识
     */
    private String key;

    /**
     * 差异属性
     * key: 属性名
     * val: 差异细节: source:xxx target:xxx
     */
    private Map<String, FieldkUnit> differentFields;

    /**
     * 对账状态
     */
    private CheckStateEnum state;

    /**
     * 调账状态
     */
    private CheckSyncEnum sync;

    @Tolerate
    public CheckUnit() {
        //do-nothing
    }

    @Override
    public String toString() {
        return "key='" + key + "', state=" + state + ", differentFields=" + differentFields;
    }
}
