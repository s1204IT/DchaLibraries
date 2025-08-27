package com.android.systemui.statusbar.policy;

/* loaded from: classes.dex */
public interface CallbackController<T> {
    void addCallback(T t);

    void removeCallback(T t);
}
