package com.android.systemui.statusbar;

import com.android.systemui.statusbar.notification.TransformState;
/* loaded from: a.zip:com/android/systemui/statusbar/TransformableView.class */
public interface TransformableView {
    TransformState getCurrentState(int i);

    void setVisible(boolean z);

    void transformFrom(TransformableView transformableView);

    void transformFrom(TransformableView transformableView, float f);

    void transformTo(TransformableView transformableView, float f);

    void transformTo(TransformableView transformableView, Runnable runnable);
}
