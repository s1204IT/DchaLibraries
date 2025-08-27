package com.google.common.collect;

/* loaded from: classes.dex */
public enum BoundType {
    OPEN { // from class: com.google.common.collect.BoundType.1
    },
    CLOSED { // from class: com.google.common.collect.BoundType.2
    };

    static BoundType forBoolean(boolean z) {
        return z ? CLOSED : OPEN;
    }
}
