package com.android.quickstep;

import android.util.SparseArray;

public class MultiStateCallback {
    private final SparseArray<Runnable> mCallbacks = new SparseArray<>();
    private int mState = 0;

    public void setState(int i) {
        Runnable runnableValueAt;
        this.mState = i | this.mState;
        int size = this.mCallbacks.size();
        for (int i2 = 0; i2 < size; i2++) {
            int iKeyAt = this.mCallbacks.keyAt(i2);
            if ((this.mState & iKeyAt) == iKeyAt && (runnableValueAt = this.mCallbacks.valueAt(i2)) != null) {
                this.mCallbacks.setValueAt(i2, null);
                runnableValueAt.run();
            }
        }
    }

    public void addCallback(int i, Runnable runnable) {
        this.mCallbacks.put(i, runnable);
    }

    public int getState() {
        return this.mState;
    }

    public boolean hasStates(int i) {
        return (this.mState & i) == i;
    }
}
