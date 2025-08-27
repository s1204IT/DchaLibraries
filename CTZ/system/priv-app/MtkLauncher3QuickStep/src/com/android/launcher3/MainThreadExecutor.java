package com.android.launcher3;

import android.os.Looper;
import com.android.launcher3.util.LooperExecutor;

/* loaded from: classes.dex */
public class MainThreadExecutor extends LooperExecutor {
    public MainThreadExecutor() {
        super(Looper.getMainLooper());
    }
}
