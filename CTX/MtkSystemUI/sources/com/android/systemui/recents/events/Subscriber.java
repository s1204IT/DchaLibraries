package com.android.systemui.recents.events;

import java.lang.ref.WeakReference;

class Subscriber {
    private WeakReference<Object> mSubscriber;
    long registrationTime;

    Subscriber(Object obj, long j) {
        this.mSubscriber = new WeakReference<>(obj);
        this.registrationTime = j;
    }

    public String toString(int i) {
        Object obj = this.mSubscriber.get();
        return obj.getClass().getSimpleName() + " [0x" + Integer.toHexString(System.identityHashCode(obj)) + ", P" + i + "]";
    }

    public Object getReference() {
        return this.mSubscriber.get();
    }
}
