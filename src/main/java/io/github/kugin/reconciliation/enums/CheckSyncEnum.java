package io.github.kugin.reconciliation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Kugin
 */
@AllArgsConstructor
@Getter
public enum CheckSyncEnum {
    SYNC("sync", "已调账"),
    NO_SYNC("no_sync", "未调账"),
    //跨日等情况忽略
    IGNORE("ignore", "忽略");
    private String val;
    private String desc;
}
