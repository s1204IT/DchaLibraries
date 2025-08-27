package com.android.systemui.shared.system;

import android.view.Choreographer;

/* loaded from: classes.dex */
public class ChoreographerCompat {
    public static void postInputFrame(Choreographer choreographer, Runnable runnable) {
        choreographer.postCallback(0, runnable, null);
    }
}
