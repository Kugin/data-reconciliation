package io.github.kugin.reconciliation.before;


/**
 * @author Kugin
 */
public interface ResourceReader {

    /**
     * @return 源数据源
     */
    ResourceLoader getSourceLoader();

    /**
     * @return 目标数据源
     */
    ResourceLoader getTargetLoader();

    /**
     * @return 差异数据源
     */
    ResourceLoader getDifferentLoader();
}
