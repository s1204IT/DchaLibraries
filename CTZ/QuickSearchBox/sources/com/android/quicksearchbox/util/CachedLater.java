package com.android.quicksearchbox.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class CachedLater<A> implements NowOrLater<A> {
    private boolean mCreating;
    private final Object mLock = new Object();
    private boolean mValid;
    private A mValue;
    private List<Consumer<? super A>> mWaitingConsumers;

    protected abstract void create();

    @Override
    public void getLater(Consumer<? super A> consumer) {
        boolean z;
        A a;
        boolean z2 = true;
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
        synchronized (this.mLock) {
            if (this.mCreating) {
                z2 = false;
            } else {
                this.mCreating = true;
            }
        }
        if (z2) {
            create();
        }
    }

    @Override
    public A getNow() {
        A a;
        synchronized (this) {
            synchronized (this.mLock) {
                if (!haveNow()) {
                    throw new IllegalStateException("getNow() called when haveNow() is false");
                }
                a = this.mValue;
            }
        }
        return a;
    }

    @Override
    public boolean haveNow() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mValid;
        }
        return z;
    }

    protected void store(A a) {
        List<Consumer<? super A>> list;
        synchronized (this.mLock) {
            this.mValue = a;
            this.mValid = true;
            this.mCreating = false;
            list = this.mWaitingConsumers;
            this.mWaitingConsumers = null;
        }
        if (list != null) {
            Iterator<Consumer<? super A>> it = list.iterator();
            while (it.hasNext()) {
                it.next().consume(a);
            }
        }
    }
}
