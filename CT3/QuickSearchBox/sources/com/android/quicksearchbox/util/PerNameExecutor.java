package com.android.quicksearchbox.util;

import java.util.HashMap;

public class PerNameExecutor implements NamedTaskExecutor {
    private final Factory<NamedTaskExecutor> mExecutorFactory;
    private HashMap<String, NamedTaskExecutor> mExecutors;

    public PerNameExecutor(Factory<NamedTaskExecutor> executorFactory) {
        this.mExecutorFactory = executorFactory;
    }

    @Override
    public synchronized void execute(NamedTask task) {
        if (this.mExecutors == null) {
            this.mExecutors = new HashMap<>();
        }
        String name = task.getName();
        NamedTaskExecutor executor = this.mExecutors.get(name);
        if (executor == null) {
            executor = this.mExecutorFactory.create();
            this.mExecutors.put(name, executor);
        }
        executor.execute(task);
    }
}
