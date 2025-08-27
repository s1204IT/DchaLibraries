package com.android.quicksearchbox.util;

import android.os.Process;
import java.util.concurrent.ThreadFactory;

/* loaded from: classes.dex */
public class PriorityThreadFactory implements ThreadFactory {
    private final int mPriority;

    public PriorityThreadFactory(int i) {
        this.mPriority = i;
    }

    @Override // java.util.concurrent.ThreadFactory
    public Thread newThread(Runnable runnable) {
        return new Thread(runnable) { // from class: com.android.quicksearchbox.util.PriorityThreadFactory.1
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() throws SecurityException, IllegalArgumentException {
                Process.setThreadPriority(PriorityThreadFactory.this.mPriority);
                super.run();
            }
        };
    }
}
