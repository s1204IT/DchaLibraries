package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

@GwtCompatible
public enum BoundType {
    OPEN {
    },
    CLOSED {
    };

    BoundType(BoundType boundType) {
        this();
    }

    public static BoundType[] valuesCustom() {
        return values();
    }

    static BoundType forBoolean(boolean inclusive) {
        return inclusive ? CLOSED : OPEN;
    }
}
