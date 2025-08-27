package com.android.systemui.recents.events;

import java.lang.ref.WeakReference;

/* compiled from: EventBus.java */
/* renamed from: com.android.systemui.recents.events.Subscriber, reason: use source file name */
/* loaded from: classes.dex */
class EventBus4 {
    private WeakReference<Object> mSubscriber;
    long registrationTime;

    EventBus4(Object obj, long j) {
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
