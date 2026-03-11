package com.android.systemui.recents.events;

class EventHandler {
    EventHandlerMethod method;
    int priority;
    Subscriber subscriber;

    EventHandler(Subscriber subscriber, EventHandlerMethod eventHandlerMethod, int i) {
        this.subscriber = subscriber;
        this.method = eventHandlerMethod;
        this.priority = i;
    }

    public String toString() {
        return this.subscriber.toString(this.priority) + " " + this.method.toString();
    }
}
