package com.android.contacts.common.util;

import android.view.View;
import android.view.ViewTreeObserver;

public class SchedulingUtils {
    public static void doOnPreDraw(final View view, final boolean drawNextFrame, final Runnable runnable) {
        ViewTreeObserver.OnPreDrawListener listener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                runnable.run();
                return drawNextFrame;
            }
        };
        view.getViewTreeObserver().addOnPreDrawListener(listener);
    }
}
