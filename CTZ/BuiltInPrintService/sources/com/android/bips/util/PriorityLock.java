package com.android.bips.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PriorityLock {
    private boolean mLocked = false;
    private List<Integer> mPriorities = new ArrayList();

    public synchronized void lock(int i) throws InterruptedException {
        if (this.mLocked) {
            this.mPriorities.add(Integer.valueOf(i));
            Collections.sort(this.mPriorities);
            while (true) {
                try {
                    if (!this.mLocked && i >= this.mPriorities.get(this.mPriorities.size() - 1).intValue()) {
                        break;
                    } else {
                        wait();
                    }
                } finally {
                    this.mPriorities.remove(Integer.valueOf(i));
                }
            }
        }
        this.mLocked = true;
    }

    public synchronized void unlock() {
        if (!this.mLocked) {
            throw new IllegalArgumentException("not locked");
        }
        this.mLocked = false;
        notifyAll();
    }
}
