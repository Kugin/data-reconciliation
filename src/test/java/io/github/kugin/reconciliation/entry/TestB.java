package io.github.kugin.reconciliation.entry;

import cn.hutool.core.map.MapUtil;
import io.github.kugin.reconciliation.annotation.CheckField;
import io.github.kugin.reconciliation.annotation.CheckIdentity;
import io.github.kugin.reconciliation.domain.CheckAdapter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * @author Kugin
 */

@Data
@AllArgsConstructor
public class TestB implements CheckAdapter {
    @CheckIdentity
    String name;
    String test;
    @CheckField
    Double amount;

    @Override
    public String getKey() {
        return name + test;
    }

    @Override
    public Map<String, Object> getCheckData() {
        return MapUtil.of("amount", amount);
    }
}
