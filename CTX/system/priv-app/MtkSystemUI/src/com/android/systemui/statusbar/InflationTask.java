package com.android.systemui.statusbar;
/* loaded from: classes.dex */
public interface InflationTask {
    void abort();

    default void supersedeTask(InflationTask inflationTask) {
    }
}
