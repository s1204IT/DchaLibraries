package com.android.server.pm;

import android.os.Binder;

class KeySetHandle extends Binder {
    private final long mId;
    private int mRefCount;

    protected KeySetHandle(long j) {
        this.mId = j;
        this.mRefCount = 1;
    }

    protected KeySetHandle(long j, int i) {
        this.mId = j;
        this.mRefCount = i;
    }

    public long getId() {
        return this.mId;
    }

    protected int getRefCountLPr() {
        return this.mRefCount;
    }

    protected void setRefCountLPw(int i) {
        this.mRefCount = i;
    }

    protected void incrRefCountLPw() {
        this.mRefCount++;
    }

    protected int decrRefCountLPw() {
        this.mRefCount--;
        return this.mRefCount;
    }
}
