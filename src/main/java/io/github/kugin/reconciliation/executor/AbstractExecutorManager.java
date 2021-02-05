package io.github.kugin.reconciliation.executor;

import lombok.Data;

/**
 * @author Kugin
 */
@Data
public abstract class AbstractExecutorManager implements ExecutorManager {

    private static final String FORMAT = "day:check:%s:%s";

    private String id;

    private String date;

    public AbstractExecutorManager(String id) {
        this.id = id;
    }

    @Override
    public void initDate(String date) {
        setDate(date);
    }

    @Override
    public String getExecutorKey() {
        return String.format(FORMAT, id, date);
    }

    @Override
    public String getProcessingKey() {
        return getExecutorKey() + "_is_processing";
    }
}
