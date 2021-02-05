package io.github.kugin.reconciliation.domain;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

/**
 * 比较单元,某一个对比属性的比较结果
 *
 * @author Kugin
 */
@Data
@Builder
public class FieldkUnit {
    /**
     * 属性名
     */
    private String fieldName;

    /**
     * 来源值
     */
    private Object source;

    /**
     * 目标值
     */
    private Object target;

    @Tolerate
    public FieldkUnit() {
        //do-nothing
    }

    @Override
    public String toString() {
        return "fileName='" + fieldName + "', source=" + source + ", target=" + target;
    }
}
