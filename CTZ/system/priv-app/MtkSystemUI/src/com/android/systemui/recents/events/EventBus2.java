package com.android.systemui.recents.events;

/* compiled from: EventBus.java */
/* renamed from: com.android.systemui.recents.events.EventHandler, reason: use source file name */
/* loaded from: classes.dex */
class EventBus2 {
    EventBus3 method;
    int priority;
    EventBus4 subscriber;

    EventBus2(EventBus4 eventBus4, EventBus3 eventBus3, int i) {
        this.subscriber = eventBus4;
        this.method = eventBus3;
        this.priority = i;
    }

    public String toString() {
        return this.subscriber.toString(this.priority) + " " + this.method.toString();
    }
}
