package com.android.systemui.statusbar.policy;

/* loaded from: classes.dex */
public interface IconLogger {
    void onIconHidden(String str);

    void onIconShown(String str);

    default void onIconVisibility(String str, boolean z) {
        if (z) {
            onIconShown(str);
        } else {
            onIconHidden(str);
        }
    }
}
