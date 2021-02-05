package io.github.kugin.reconciliation.domain;

import java.util.Map;

/**
 * @author Kugin
 *E CheckEntry 的包装类,为了解决对比实体关系复杂的问题(1.唯一标识存在多个字段拼接,2.两个不同类中的对比字段的值不一致,比如A类中的"1"要和B类中的"b"识别为一致)
 */
public interface CheckAdapter {

    /**
     * 唯一标识,可能存在多个字段拼接
     */
    String getKey();

    /**
     * 比较数据
     */
    Map<String, Object> getCheckData();

}
