package com.google.common.collect;

public enum BoundType {
    OPEN {
        @Override
        BoundType flip() {
            return CLOSED;
        }
    },
    CLOSED {
        @Override
        BoundType flip() {
            return OPEN;
        }
    };

    abstract BoundType flip();

    static BoundType forBoolean(boolean z) {
        return z ? CLOSED : OPEN;
    }
}
