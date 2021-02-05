package io.github.kugin.reconciliation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Kugin
 */
@AllArgsConstructor
@Getter
public enum CheckStateEnum {
    SOURCE_MORE("source_more", "上游多账"),
    TARGET_MORE("target_more", "下游多账"),
    DIFFERENT("different", "存在差异");
    private String val;
    private String desc;
}
