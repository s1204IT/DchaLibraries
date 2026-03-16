package com.android.quicksearchbox.util;

import java.util.ArrayList;
import java.util.List;

public abstract class CachedLater<A> implements NowOrLater<A> {
    private boolean mCreating;
    private final Object mLock = new Object();
    private boolean mValid;
    private A mValue;
    private List<Consumer<? super A>> mWaitingConsumers;

    protected abstract void create();

    protected void store(A value) {
        List<Consumer<? super A>> waitingConsumers;
        synchronized (this.mLock) {
            this.mValue = value;
            this.mValid = true;
            this.mCreating = false;
            waitingConsumers = this.mWaitingConsumers;
            this.mWaitingConsumers = null;
        }
        if (waitingConsumers != null) {
            for (Consumer<? super A> consumer : waitingConsumers) {
                consumer.consume(value);
            }
        }
    }

    @Override
    public void getLater(Consumer<? super A> consumer) {
        boolean z;
        A a;
        synchronized (this.mLock) {
            z = this.mValid;
            a = this.mValue;
            if (!z) {
                if (this.mWaitingConsumers == null) {
                    this.mWaitingConsumers = new ArrayList();
                }
                this.mWaitingConsumers.add(consumer);
            }
        }
        if (z) {
            consumer.consume(a);
            return;
        }
        boolean z2 = false;
        synchronized (this.mLock) {
            if (!this.mCreating) {
                this.mCreating = true;
                z2 = true;
            }
        }
        if (z2) {
            create();
        }
    }

    @Override
    public boolean haveNow() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mValid;
        }
        return z;
    }

    @Override
    public synchronized A getNow() {
        A a;
        synchronized (this.mLock) {
            if (!haveNow()) {
                throw new IllegalStateException("getNow() called when haveNow() is false");
            }
            a = this.mValue;
        }
        return a;
    }
}
