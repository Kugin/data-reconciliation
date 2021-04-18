package io.github.kugin.reconciliation.before;

import io.github.kugin.reconciliation.domain.CheckContext;

import java.util.Objects;

/**
 * 默认前置处理器,处理前置校验与对账资源准备
 *
 * @author Kugin
 */
public class DefaultBeforeCheckProcessor extends AbstractBeforeCheckProcessor {

    /**
     * 资源读取
     */
    private final ResourceReader resourceReader;

    /**
     * 前置校验
     */
    private final CheckPre checkPre;

    public DefaultBeforeCheckProcessor(ResourceReader resourceReader, CheckPre checkPre) {
        this.resourceReader = Objects.requireNonNull(resourceReader, "数据源为空");
        this.checkPre = checkPre;
    }

    @Override
    public boolean doPre(CheckContext context) {
        return Objects.isNull(checkPre) || checkPre.doPre(context);
    }

    @Override
    public ResourceLoader getSourceLoader() {
        return Objects.requireNonNull(resourceReader.getSourceLoader(), "源数据源为空");
    }

    @Override
    public ResourceLoader getTargetLoader() {
        return Objects.requireNonNull(resourceReader.getTargetLoader(), "目标数据源为空");
    }

    @Override
    public ResourceLoader getDifferentLoader() {
        ResourceReader reader = Objects.requireNonNull(resourceReader, "数据源为空");
        //差异数据源可以为空,重复时全量
        return reader.getDifferentLoader();
    }
}
