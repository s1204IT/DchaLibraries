package com.android.quicksearchbox.util;

import java.util.HashMap;

/* loaded from: classes.dex */
public class PerNameExecutor implements NamedTaskExecutor {
    private final Factory<NamedTaskExecutor> mExecutorFactory;
    private HashMap<String, NamedTaskExecutor> mExecutors;

    public PerNameExecutor(Factory<NamedTaskExecutor> factory) {
        this.mExecutorFactory = factory;
    }

    @Override // com.android.quicksearchbox.util.NamedTaskExecutor
    public synchronized void execute(NamedTask namedTask) {
        if (this.mExecutors == null) {
            this.mExecutors = new HashMap<>();
        }
        String name = namedTask.getName();
        NamedTaskExecutor namedTaskExecutorCreate = this.mExecutors.get(name);
        if (namedTaskExecutorCreate == null) {
            namedTaskExecutorCreate = this.mExecutorFactory.create();
            this.mExecutors.put(name, namedTaskExecutorCreate);
        }
        namedTaskExecutorCreate.execute(namedTask);
    }
}
