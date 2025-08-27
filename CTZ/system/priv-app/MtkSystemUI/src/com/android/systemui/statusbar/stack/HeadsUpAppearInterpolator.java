package com.android.systemui.statusbar.stack;

import android.graphics.Path;
import android.view.animation.PathInterpolator;

/* loaded from: classes.dex */
public class HeadsUpAppearInterpolator extends PathInterpolator {
    private static float X1 = 250.0f;
    private static float X2 = 200.0f;
    private static float XTOT = X1 + X2;

    public HeadsUpAppearInterpolator() {
        super(getAppearPath());
    }

    private static Path getAppearPath() {
        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.cubicTo((X1 * 0.8f) / XTOT, 1.125f, (X1 * 0.8f) / XTOT, 1.125f, X1 / XTOT, 1.125f);
        path.cubicTo((X1 + (X2 * 0.4f)) / XTOT, 1.125f, (X1 + (X2 * 0.2f)) / XTOT, 1.0f, 1.0f, 1.0f);
        return path;
    }

    public static float getFractionUntilOvershoot() {
        return X1 / XTOT;
    }
}
