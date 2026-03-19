package com.android.quicksearchbox.util;

import android.os.Process;
import java.util.concurrent.ThreadFactory;

public class PriorityThreadFactory implements ThreadFactory {
    private final int mPriority;

    public PriorityThreadFactory(int i) {
        this.mPriority = i;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        return new Thread(runnable) {
            @Override
            public void run() {
                Process.setThreadPriority(PriorityThreadFactory.this.mPriority);
                super.run();
            }
        };
    }
}
