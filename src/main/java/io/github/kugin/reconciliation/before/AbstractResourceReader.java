package io.github.kugin.reconciliation.before;

/**
 * @author Kugin
 */
public abstract class AbstractResourceReader implements ResourceReader {

    private ResourceLoader sourceLoader;

    private ResourceLoader targetLoader;

    private ResourceLoader differentLoader;

    public AbstractResourceReader(ResourceLoader sourceLoader, ResourceLoader targetLoader) {
        this.sourceLoader = sourceLoader;
        this.targetLoader = targetLoader;
    }

    public AbstractResourceReader(ResourceLoader sourceLoader, ResourceLoader targetLoader, ResourceLoader differentLoader) {
        this.sourceLoader = sourceLoader;
        this.targetLoader = targetLoader;
        this.differentLoader = differentLoader;
    }

    @Override
    public ResourceLoader getSourceLoader() {
        return sourceLoader;
    }

    @Override
    public ResourceLoader getTargetLoader() {
        return targetLoader;
    }

    @Override
    public ResourceLoader getDifferentLoader() {
        return differentLoader;
    }
}
