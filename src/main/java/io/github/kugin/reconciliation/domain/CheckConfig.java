package io.github.kugin.reconciliation.domain;

import io.github.kugin.reconciliation.after.AfterCheckProcessor;
import io.github.kugin.reconciliation.after.CheckAfter;
import io.github.kugin.reconciliation.after.CheckSync;
import io.github.kugin.reconciliation.before.BeforeCheckProcessor;
import io.github.kugin.reconciliation.before.CheckPre;
import io.github.kugin.reconciliation.before.ResourceLoader;
import io.github.kugin.reconciliation.before.ResourceReader;
import io.github.kugin.reconciliation.check.CheckProcessor;
import lombok.Builder;
import lombok.Data;

/**
 * 对账配置
 *
 * @author Kugin
 */
@Data
@Builder
public class CheckConfig {
    /**
     * 对账唯一标识:进度管理以id为主
     * name与id不可同时为空
     */
    private String id;

    /**
     * 对账名称: 名称可能重复
     */
    private String name;

    //前置处理器配置-----------------------------------

    /**
     * 自定义前置处理器:可不依赖于ResourceReader
     */
    private BeforeCheckProcessor beforeCheckProcessor;

    /**
     * 前置校验: 可为空
     */
    private CheckPre checkPre;

    /**
     * 上游数据源
     */
    private ResourceLoader srcLoader;

    /**
     * 下游数据源
     */
    private ResourceLoader targetLoader;

    /**
     * 数据源读取 (包含上下游)
     */
    private ResourceReader resourceReader;

    //对账处理器配置-----------------------------------

    /**
     * 自定义对账处理器
     */
    private CheckProcessor checkProcessor;

    //后置处理器配置-----------------------------------

    /**
     * 自定义后置处理器: 可不依赖于CheckSync
     */
    private AfterCheckProcessor afterCheckProcessor;

    /**
     * 对账结果同步处理器: 可为空
     */
    private CheckSync checkSync;

    /**
     * 对账后置处理逻辑: 可为空
     */
    private CheckAfter checkAfter;
}
