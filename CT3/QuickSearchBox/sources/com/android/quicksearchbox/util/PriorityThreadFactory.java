package com.android.quicksearchbox.util;

import android.os.Process;
import java.util.concurrent.ThreadFactory;

public class PriorityThreadFactory implements ThreadFactory {
    private final int mPriority;

    public PriorityThreadFactory(int priority) {
        this.mPriority = priority;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r) {
            @Override
            public void run() {
                Process.setThreadPriority(PriorityThreadFactory.this.mPriority);
                super.run();
            }
        };
    }
}
