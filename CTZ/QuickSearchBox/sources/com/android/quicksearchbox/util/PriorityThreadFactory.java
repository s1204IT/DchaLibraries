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
        return new Thread(this, runnable) {
            final PriorityThreadFactory this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void run() {
                Process.setThreadPriority(this.this$0.mPriority);
                super.run();
            }
        };
    }
}
