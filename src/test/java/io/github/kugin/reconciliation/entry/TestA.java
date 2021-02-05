package io.github.kugin.reconciliation.entry;

import cn.hutool.core.map.MapUtil;
import io.github.kugin.reconciliation.domain.CheckAdapter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * @author Kugin
 */
@Data
@AllArgsConstructor
public class TestA implements CheckAdapter {
    String name;
    String remark;
    Double amount;

    @Override
    public String getKey() {
        return name + remark;
    }

    @Override
    public Map<String, Object> getCheckData() {
        return MapUtil.of("amount", amount);
    }
}
