package com.android.server.accessibility;

abstract class BaseEventStreamTransformation implements EventStreamTransformation {
    private EventStreamTransformation mNext;

    BaseEventStreamTransformation() {
    }

    @Override
    public void setNext(EventStreamTransformation eventStreamTransformation) {
        this.mNext = eventStreamTransformation;
    }

    @Override
    public EventStreamTransformation getNext() {
        return this.mNext;
    }
}
