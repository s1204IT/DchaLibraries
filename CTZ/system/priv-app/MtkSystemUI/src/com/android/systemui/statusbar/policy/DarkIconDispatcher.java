package com.android.systemui.statusbar.policy;

import android.graphics.Rect;
import android.view.View;
import com.android.systemui.statusbar.phone.LightBarTransitionsController;

/* loaded from: classes.dex */
public interface DarkIconDispatcher {
    public static final Rect sTmpRect = new Rect();
    public static final int[] sTmpInt2 = new int[2];

    public interface DarkReceiver {
        void onDarkChanged(Rect rect, float f, int i);
    }

    void addDarkReceiver(DarkReceiver darkReceiver);

    void applyDark(DarkReceiver darkReceiver);

    LightBarTransitionsController getTransitionsController();

    void removeDarkReceiver(DarkReceiver darkReceiver);

    void setIconsDarkArea(Rect rect);

    static int getTint(Rect rect, View view, int i) {
        if (isInArea(rect, view)) {
            return i;
        }
        return -1;
    }

    static float getDarkIntensity(Rect rect, View view, float f) {
        if (isInArea(rect, view)) {
            return f;
        }
        return 0.0f;
    }

    static boolean isInArea(Rect rect, View view) {
        if (rect.isEmpty()) {
            return true;
        }
        sTmpRect.set(rect);
        view.getLocationOnScreen(sTmpInt2);
        int i = sTmpInt2[0];
        return (2 * Math.max(0, Math.min(i + view.getWidth(), rect.right) - Math.max(i, rect.left)) > view.getWidth()) && (rect.top <= 0);
    }
}
