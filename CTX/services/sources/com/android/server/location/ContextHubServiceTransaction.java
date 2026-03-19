package com.android.server.location;

import android.hardware.location.ContextHubTransaction;
import android.hardware.location.NanoAppState;
import java.util.List;
import java.util.concurrent.TimeUnit;

abstract class ContextHubServiceTransaction {
    private boolean mIsComplete = false;
    private final int mTransactionId;
    private final int mTransactionType;

    abstract int onTransact();

    ContextHubServiceTransaction(int i, int i2) {
        this.mTransactionId = i;
        this.mTransactionType = i2;
    }

    void onTransactionComplete(int i) {
    }

    void onQueryResponse(int i, List<NanoAppState> list) {
    }

    int getTransactionId() {
        return this.mTransactionId;
    }

    int getTransactionType() {
        return this.mTransactionType;
    }

    long getTimeout(TimeUnit timeUnit) {
        if (this.mTransactionType == 0) {
            return timeUnit.convert(30L, TimeUnit.SECONDS);
        }
        return timeUnit.convert(5L, TimeUnit.SECONDS);
    }

    void setComplete() {
        this.mIsComplete = true;
    }

    boolean isComplete() {
        return this.mIsComplete;
    }

    public String toString() {
        return ContextHubTransaction.typeToString(this.mTransactionType, true) + " transaction (ID = " + this.mTransactionId + ")";
    }
}
