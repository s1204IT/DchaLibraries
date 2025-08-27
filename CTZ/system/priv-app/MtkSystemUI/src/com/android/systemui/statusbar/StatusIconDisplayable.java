package com.android.systemui.statusbar;

import com.android.systemui.statusbar.policy.DarkIconDispatcher;

/* loaded from: classes.dex */
public interface StatusIconDisplayable extends DarkIconDispatcher.DarkReceiver {
    String getSlot();

    int getVisibleState();

    boolean isIconVisible();

    void setDecorColor(int i);

    void setStaticDrawableColor(int i);

    void setVisibleState(int i);

    default boolean isIconBlocked() {
        return false;
    }
}
