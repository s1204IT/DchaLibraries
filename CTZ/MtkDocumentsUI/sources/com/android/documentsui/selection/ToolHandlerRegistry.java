package com.android.documentsui.selection;

import android.support.v4.util.Preconditions;
import android.view.MotionEvent;
import java.util.Arrays;
import java.util.List;

final class ToolHandlerRegistry<T> {
    private final T mDefault;
    private final List<T> mHandlers = Arrays.asList(null, null, null, null, null);

    ToolHandlerRegistry(T t) {
        Preconditions.checkArgument(t != null);
        this.mDefault = t;
        for (int i = 0; i < 5; i++) {
            this.mHandlers.set(i, null);
        }
    }

    void set(int i, T t) {
        Preconditions.checkArgument(i >= 0 && i <= 4);
        Preconditions.checkState(this.mHandlers.get(i) == null);
        this.mHandlers.set(i, t);
    }

    T get(MotionEvent motionEvent) {
        T t = this.mHandlers.get(motionEvent.getToolType(0));
        return t != null ? t : this.mDefault;
    }
}
